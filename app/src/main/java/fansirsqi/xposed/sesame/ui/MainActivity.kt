package fansirsqi.xposed.sesame.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.data.RunType
import fansirsqi.xposed.sesame.data.UIConfig
import fansirsqi.xposed.sesame.data.ViewAppInfo
import fansirsqi.xposed.sesame.data.ViewAppInfo.verifyId
import fansirsqi.xposed.sesame.entity.FriendWatch
import fansirsqi.xposed.sesame.entity.UserEntity
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.util.AssetUtil
import fansirsqi.xposed.sesame.util.Detector
import fansirsqi.xposed.sesame.util.DeviceInfoCard
import fansirsqi.xposed.sesame.util.DeviceInfoUtil
import fansirsqi.xposed.sesame.util.FansirsqiUtil
import fansirsqi.xposed.sesame.newui.WatermarkView
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.PermissionUtil
import fansirsqi.xposed.sesame.util.ToastUtil
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//   欢迎自己打包 欢迎大佬pr
//   项目开源且公益  维护都是自愿
//   但是如果打包改个名拿去卖钱忽悠小白
//   那我只能说你妈死了 就当开源项目给你妈烧纸钱了
class MainActivity : BaseActivity() {
    private val TAG = "MainActivity"
    private var hasPermissions = false
    private var userNameArray = arrayOf("默认")
    private var userEntityArray = arrayOf<UserEntity?>(null)
    private lateinit var oneWord: TextView

    @SuppressLint("SetTextI18n", "UnsafeDynamicallyLoadedCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ToastUtil.init(this) // 初始化全局 Context

        hasPermissions = PermissionUtil.checkOrRequestFilePermissions(this)
        if (!hasPermissions) {
            Toast.makeText(this, "未获取文件读写权限", Toast.LENGTH_LONG).show()
            finish() // 如果权限未获取，终止当前 Activity
            return
        }
        setContentView(R.layout.activity_main)
        oneWord = findViewById(R.id.one_word)
        val deviceInfo: ComposeView = findViewById(R.id.device_info)
        val v = WatermarkView.install(this)
        deviceInfo.setContent {
            val customColorScheme = lightColorScheme(
                primary = Color(0x00FFFFFF), onPrimary = Color.White, background = Color(0x00FFFFFF), onBackground = Color.White
            )
            MaterialTheme(colorScheme = customColorScheme) {
                DeviceInfoCard(DeviceInfoUtil.showInfo(verifyId))
            }
        }
        // 获取并设置一言句子
        try {
            if (!AssetUtil.copySoFileToStorage(this, AssetUtil.checkerDestFile)) {
                Log.error(TAG, "checker file copy failed")
            }
            if (!AssetUtil.copySoFileToStorage(this, AssetUtil.dexkitDestFile)) {
                Log.error(TAG, "dexkit file copy failed")
            }
            Detector.loadLibrary("checker")
            Detector.initDetector(this)
        } catch (e: Exception) {
            Log.error(TAG, "load libSesame err:" + e.message)
        }
        lifecycleScope.launch {
            val result = FansirsqiUtil.getOneWord()
            oneWord.text = result
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasPermissions) {
            try {
                UIConfig.load()
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
            try {
                val userNameList: MutableList<String> = ArrayList()
                val userEntityList: MutableList<UserEntity?> = ArrayList()
                val configFiles = Files.CONFIG_DIR.listFiles()
                if (configFiles != null) {
                    for (configDir in configFiles) {
                        if (configDir.isDirectory) {
                            val userId = configDir.name
                            UserMap.loadSelf(userId)
                            val userEntity = UserMap.get(userId)
                            val userName = if (userEntity == null) {
                                userId
                            } else {
                                userEntity.showName + ": " + userEntity.account
                            }
                            userNameList.add(userName)
                            userEntityList.add(userEntity)
                        }
                    }
                }
                userNameList.add(0, "默认")
                userEntityList.add(0, null)
                userNameArray = userNameList.toTypedArray()
                userEntityArray = userEntityList.toTypedArray()
            } catch (e: Exception) {
                userNameArray = arrayOf("默认")
                userEntityArray = arrayOf(null)
                Log.printStackTrace(e)
            }
        }
        updateSubTitle(RunType.LOADED.nickName)
    }

    fun onClick(v: View) {
        var data = "file://"
        val id = v.id
        when (id) {
            R.id.btn_forest_log -> {
                data += Files.getForestLogFile().absolutePath
            }
            R.id.btn_farm_log -> {
                data += Files.getFarmLogFile().absolutePath
            }
            R.id.btn_view_error_log_file -> {
                data += Files.getErrorLogFile().absolutePath
            }
            R.id.btn_view_all_log_file -> {
                data += Files.getRecordLogFile().absolutePath
            }
            R.id.btn_github -> {
                data = "https://github.com/Fansirsqi/Sesame-TK"
            }
            R.id.btn_settings -> {
                showSelectionDialog(
                    "📌 请选择配置",
                    userNameArray,
                    { index: Int -> this.goSettingActivity(index) },
                    "😡 老子就不选",
                    {},
                    true
                )
                return
            }
            R.id.one_word -> {
                oneWord.text = "😡 正在获取句子，请稍后……"
                lifecycleScope.launch {
                    val result = FansirsqiUtil.getOneWord()
                    oneWord.text = result
                }
                return
            }
        }
        val it = Intent(this, HtmlViewerActivity::class.java)
        it.data = data.toUri()
        startActivity(it)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            val aliasComponent = ComponentName(this, General.MODULE_PACKAGE_UI_ICON)
            val state = packageManager.getComponentEnabledSetting(aliasComponent)
            val isEnabled = state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            menu.add(0, 1, 1, R.string.hide_the_application_icon)
                .setCheckable(true).isChecked = !isEnabled

            menu.add(0, 2, 2, R.string.friend_watch)
            menu.add(0, 3, 3, R.string.other_log)
            menu.add(0, 4, 4, R.string.view_error_log_file)
            menu.add(0, 5, 5, R.string.view_all_log_file)
            menu.add(0, 6, 6, R.string.view_runtim_log_file)
            menu.add(0, 7, 7, R.string.view_capture)
            menu.add(0, 8, 8, R.string.extend)
            menu.add(0, 9, 9, R.string.settings)
            if (BuildConfig.DEBUG) {
                menu.add(0, 10, 10, "清除配置")
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
            ToastUtil.makeText(this, "菜单创建失败，请重试", Toast.LENGTH_SHORT).show()
            return false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> { // 隐藏应用图标
                val shouldHide = !item.isChecked
                item.isChecked = shouldHide
                val aliasComponent = ComponentName(this, General.MODULE_PACKAGE_UI_ICON)
                val newState = if (shouldHide) {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }
                packageManager.setComponentEnabledSetting(
                    aliasComponent,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
                Toast.makeText(this, "设置已保存，可能需要重启桌面才能生效", Toast.LENGTH_SHORT).show()
                return true
            }
            2 -> { // 好友关注列表
                showSelectionDialog(
                    "🤣 请选择有效账户[别选默认]",
                    userNameArray,
                    { index: Int -> this.goFriendWatch(index) },
                    "😡 老子不选了，滚",
                    {},
                    false
                )
                return true
            }
            3 -> { // 查看其他日志
                val data = "file://" + Files.getOtherLogFile().absolutePath
                val intent = Intent(this, HtmlViewerActivity::class.java)
                intent.putExtra("nextLine", false)
                intent.putExtra("canClear", true)
                intent.data = data.toUri()
                startActivity(intent)
                return true
            }
            4 -> { // 查看错误日志文件
                val errorData = "file://" + Files.getErrorLogFile().absolutePath
                val errorIt = Intent(this, HtmlViewerActivity::class.java)
                errorIt.putExtra("nextLine", false)
                errorIt.putExtra("canClear", true)
                errorIt.data = errorData.toUri()
                startActivity(errorIt)
                return true
            }
            5 -> { // 查看全部日志文件
                val recordData = "file://" + Files.getRecordLogFile().absolutePath
                val otherIt = Intent(this, HtmlViewerActivity::class.java)
                otherIt.putExtra("nextLine", false)
                otherIt.putExtra("canClear", true)
                otherIt.data = recordData.toUri()
                startActivity(otherIt)
                return true
            }
            6 -> { // 查看运行时日志文件
                val runtimeData = "file://" + Files.getRuntimeLogFile().absolutePath
                val allIt = Intent(this, HtmlViewerActivity::class.java)
                allIt.putExtra("nextLine", false)
                allIt.putExtra("canClear", true)
                allIt.data = runtimeData.toUri()
                startActivity(allIt)
                return true
            }
            7 -> { // 查看截图
                val captureData = "file://" + Files.getCaptureLogFile().absolutePath
                val captureIt = Intent(this, HtmlViewerActivity::class.java)
                captureIt.putExtra("nextLine", false)
                captureIt.putExtra("canClear", true)
                captureIt.data = captureData.toUri()
                startActivity(captureIt)
                return true
            }
            8 -> { // 扩展
                startActivity(Intent(this, ExtendActivity::class.java))
                return true
            }
            9 -> { // 设置
                selectSettingUid()
                return true
            }
            10 -> { // 清除配置
                AlertDialog.Builder(this)
                    .setTitle("⚠️ 警告")
                    .setMessage("🤔 确认清除所有模块配置？")
                    .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                        if (Files.delFile(Files.CONFIG_DIR)) {
                            Toast.makeText(this, "🙂 清空配置成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "😭 清空配置失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                    .create()
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectSettingUid() {
        val latch = CountDownLatch(1)
        val dialog = StringDialog.showSelectionDialog(
            this,
            "📌 请选择配置",
            userNameArray,
            { dialog1: DialogInterface, which: Int ->
                goSettingActivity(which)
                dialog1.dismiss()
                latch.countDown()
            },
            "返回",
            { dialog1: DialogInterface ->
                dialog1.dismiss()
                latch.countDown()
            })

        val length = userNameArray.size
        if (length in 1..2) {
            val timeoutMillis: Long = 800
            Thread {
                try {
                    if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        runOnUiThread {
                            if (dialog.isShowing) {
                                goSettingActivity(length - 1)
                                dialog.dismiss()
                            }
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }.start()
        }
    }

    private fun showSelectionDialog(
        title: String?,
        options: Array<String>,
        onItemSelected: Consumer<Int>,
        negativeButtonText: String?,
        onNegativeButtonClick: Runnable,
        showDefaultOption: Boolean
    ) {
        val latch = CountDownLatch(1)
        val dialog = StringDialog.showSelectionDialog(
            this,
            title,
            options,
            { dialog1: DialogInterface, which: Int ->
                onItemSelected.accept(which)
                dialog1.dismiss()
                latch.countDown()
            },
            negativeButtonText,
            { dialog1: DialogInterface ->
                onNegativeButtonClick.run()
                dialog1.dismiss()
                latch.countDown()
            })

        val length = options.size
        if (showDefaultOption && length > 0 && length < 3) {
            val timeoutMillis: Long = 800
            Thread {
                try {
                    if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        runOnUiThread {
                            if (dialog.isShowing) {
                                onItemSelected.accept(length - 1)
                                dialog.dismiss()
                            }
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }.start()
        }
    }

    private fun goFriendWatch(index: Int) {
        val userEntity = userEntityArray[index]
        if (userEntity != null) {
            ListDialog.show(
                this,
                getString(R.string.friend_watch),
                FriendWatch.getList(userEntity.userId),
                SelectModelFieldFunc.newMapInstance(),
                false,
                ListDialog.ListType.SHOW
            )
        } else {
            ToastUtil.makeText(this, "😡 别他妈选默认！！！！！！！！", Toast.LENGTH_LONG).show()
        }
    }

    private fun goSettingActivity(index: Int) {
        if (Detector.loadLibrary("checker")) {
            val userEntity = userEntityArray[index]
            val targetActivity = UIConfig.INSTANCE.targetActivityClass
            val intent = Intent(this, targetActivity)
            if (userEntity != null) {
                intent.putExtra("userId", userEntity.userId)
                intent.putExtra("userName", userEntity.showName)
            } else {
                intent.putExtra("userName", userNameArray[index])
            }
            startActivity(intent)
        } else {
            Detector.tips(this, "缺少必要依赖！")
        }
    }

    fun updateSubTitle(runType: String) {
        Log.runtime(TAG, "updateSubTitle$runType")
        baseTitle = ViewAppInfo.appTitle + "[" + runType + "]"
        when (runType) {
            RunType.DISABLE.nickName -> setBaseTitleTextColor(ContextCompat.getColor(this, R.color.not_active_text))
            RunType.ACTIVE.nickName -> setBaseTitleTextColor(ContextCompat.getColor(this, R.color.active_text))
            RunType.LOADED.nickName -> setBaseTitleTextColor(ContextCompat.getColor(this, R.color.textColorPrimary))
        }
    }
}
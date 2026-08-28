package com.example.workprofiledpc

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 이 앱은 두 가지 모드로 동작합니다.
 *
 *  1) 개인 프로필(개인 사용자)에서 실행될 때
 *     → "직장 프로필(격리 공간) 프로비저닝" 온보딩 화면.
 *        ACTION_PROVISION_MANAGED_PROFILE 을 띄워 시스템이 새 직장 프로필을
 *        만들도록 지시합니다. Device Owner가 될 필요가 없습니다.
 *        시스템이 이 앱을 직장 프로필 안으로 복사해 그 프로필의
 *        Profile Owner(직장 관리자)로 지정합니다.
 *
 *  2) 직장 프로필(관리형 프로필) 안에서 실행될 때
 *     → 이 앱이 바로 그 프로필의 Profile Owner 입니다.
 *        직장 프로필 잠금(보안폴더식 비밀번호)을 설정/적용하고
 *        격리 공간을 관리합니다.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private lateinit var personalPanel: View
    private lateinit var ownerPanel: View
    private lateinit var personalStatus: TextView
    private lateinit var ownerStatus: TextView
    private lateinit var ownerBadge: TextView

    companion object {
        private const val REQUEST_PROVISION = 1001
        private const val TAG = "WorkProfileDPC"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(DevicePolicyManager::class.java)
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        personalPanel = findViewById(R.id.personalPanel)
        ownerPanel = findViewById(R.id.ownerPanel)
        personalStatus = findViewById(R.id.personalStatus)
        ownerStatus = findViewById(R.id.ownerStatus)
        ownerBadge = findViewById(R.id.ownerBadge)

        findViewById<Button>(R.id.btnProvision).setOnClickListener { provisionWorkProfile() }
        findViewById<Button>(R.id.btnOpenWorkSettings).setOnClickListener { openWorkSettings() }
        findViewById<Button>(R.id.btnSetLock).setOnClickListener { setWorkProfileLock() }
        findViewById<Button>(R.id.btnLockNow).setOnClickListener { lockNow() }
        findViewById<Button>(R.id.btnReleaseAdmin).setOnClickListener { releaseAdmin() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** 지금 이 앱 인스턴스가 직장 프로필(관리형 프로필) 안에서 실행 중인지 */
    private fun isManagedProfileContext(): Boolean {
        return dpm.isProfileOwnerApp(packageName) ||
            getSystemService(UserManager::class.java).isManagedProfile
    }

    private fun refresh() {
        if (isManagedProfileContext()) {
            // ---- 직장 프로필 안 / 이 앱 = Profile Owner ----
            personalPanel.visibility = View.GONE
            ownerPanel.visibility = View.VISIBLE
            val hasLock = dpm.getPasswordQuality(adminComponent) !=
                DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED
            ownerBadge.text = if (hasLock) "직장 프로필 잠금 · 활성" else "직장 프로필 잠금 · 미설정"
            ownerBadge.setBackgroundColor(
                if (hasLock) getColor(R.color.brand_dark) else getColor(R.color.lock_off)
            )
            ownerStatus.text =
                "이 앱은 직장(격리) 프로필의 관리자(Profile Owner)입니다.\n" +
                "개인 프로필에는 어떤 관리자 권한도 부여되지 않았습니다."
        } else {
            // ---- 개인 프로필 안 ----
            personalPanel.visibility = View.VISIBLE
            ownerPanel.visibility = View.GONE
            personalStatus.text =
                "이 앱은 개인 프로필(원래 앱)에서 실행 중입니다.\n" +
                "관리자 권한은 직장(격리) 프로필 안의 앱만 가지며, 개인 앱에는 없습니다.\n" +
                "아래 버튼으로 직장 프로필을 생성하세요. 개인 데이터는 건드리지 않습니다."
        }
    }

    // ===================================================================
    //  개인 프로필 : 직장 프로필 생성 (DPC-first profile owner provisioning)
    // ===================================================================
    private fun provisionWorkProfile() {
        // 기존 직장 프로필이 이미 있으면 시스템이 프로비저닝을 거부하므로,
        // 필요한 경우 시스템이 오류를 보여줍니다. 여기서는 바로 프로비저닝을 요청합니다.
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
        intent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            adminComponent.flattenToString()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 사용자에게 프로필 생성 동의를 받습니다 (제거하면 자동 진행)
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_USER_CONSENT, false)
        }

        if (intent.resolveActivity(packageManager) != null) {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_PROVISION)
        } else {
            Toast.makeText(this, "이 기기에서 관리형 직장 프로필을 지원하지 않습니다.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROVISION) {
            val ok = resultCode == Activity.RESULT_OK
            Toast.makeText(
                this,
                if (ok) "직장 프로필 생성 완료!\n관리자 앱 아이콘이 런처에 나타납니다."
                else "직장 프로필 생성이 취소/실패했습니다.",
                Toast.LENGTH_LONG
            ).show()
            refresh()
        }
    }

    private fun openWorkSettings() {
        // 개인 프로필에서는 시스템 설정을 열어 직장 프로필 보안 설정을 안내합니다.
        Toast.makeText(
            this,
            "설정 > 보안 및 개인정보 > 직장 프로필 보안에서 잠금 유형을 선택하세요.",
            Toast.LENGTH_LONG
        ).show()
        runCatching { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
    }

    // ===================================================================
    //  직장 프로필(Profile Owner) : 보안폴더식 잠금 / 관리
    // ===================================================================
    private fun setWorkProfileLock() {
        try {
            // 직장 프로필 전용 잠금(비밀번호) 정책을 강제합니다.
            dpm.setPasswordQuality(adminComponent, DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC)
            dpm.setPasswordMinimumLength(adminComponent, 4)
            dpm.setPasswordMinimumLetters(adminComponent, 1)
            dpm.setMaximumTimeToLock(adminComponent, 30_000) // 30초 비활성 시 직장 프로필 잠금
            Toast.makeText(
                this,
                "직장 프로필 잠금 정책을 적용했습니다.\n이제 비밀번호를 설정하세요.",
                Toast.LENGTH_LONG
            ).show()
            // 직장 프로필의 비밀번호를 설정하는 화면을 띄웁니다.
            startActivity(Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD))
        } catch (e: Exception) {
            Log.e(TAG, "setWorkProfileLock 실패", e)
            Toast.makeText(this, "잠금 설정 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun lockNow() {
        runCatching {
            dpm.lockNow()
            Toast.makeText(this, "직장 프로필을 잠갔습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /** Profile Owner 자격 해제 (격리 공간은 그대로 두고 관리자만 해제) */
    private fun releaseAdmin() {
        try {
            dpm.removeActiveAdmin(adminComponent)
            Toast.makeText(this, "관리자(Profile Owner)를 해제했습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "releaseAdmin 실패", e)
            Toast.makeText(this, "해제 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

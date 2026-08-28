package com.example.workprofiledpc

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

/**
 * DevicePolicyController(DPC) 리시버.
 *
 * 직장 프로필을 프로비저닝할 때 시스템이 이 컴포넌트를 Profile Owner 로 지정하므로,
 * 직장 프로필 안에서만 초기 정책이 적용됩니다. 개인 프로필에서는 어떤 관리 권한도
 * 가지지 않습니다. (Device Owner가 아닙니다.)
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "직장 프로필 관리자로 활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "직장 프로필 관리자 권한이 해제되었습니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * 직장 프로필 프로비저닝 완료 시 호출됩니다. (직장 프로필 안에서 실행됨)
     * 이때 초기 보안 정책을 적용해 "보안폴더식 잠금"을 즉시 활성화합니다.
     */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, AdminReceiver::class.java)

        try {
            // 직장 프로필 잠금(비밀번호) 정책 -> 직장 앱을 열 때 비밀번호 요구
            dpm.setPasswordQuality(admin, DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC)
            dpm.setPasswordMinimumLength(admin, 4)
            dpm.setPasswordMinimumLetters(admin, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 프로필이 비활성일 때 잠금 (보안폴더처럼)
                dpm.setMaximumTimeToLock(admin, 30_000)
            }
        } catch (e: Exception) {
            // 정책 적용 실패가 치명적이진 않음
        }

        Toast.makeText(
            context,
            "직장 프로필 준비 완료!\n보안을 위해 비밀번호를 설정하세요.",
            Toast.LENGTH_LONG
        ).show()
    }
}

# WorkProfile DPC (TestDPC 스타일 · 직장 프로필 격리 앱)

Google의 **TestDPC**(Device Policy Controller)처럼 동작하는 Android 앱. 님이 원하신 구조 그대로입니다:

> **개인 프로필(원래 앱)에 관리자 권한을 주지 않고, 용도는 격리된 직장 프로필 안의 이 앱이 그 프로필의 관리자(Profile Owner)가 되게** 합니다. 직장 프로필은 안드로이드의 정식 기능으로, **운영체제 본체와 완전히 분리된 별도의 Android 공간**입니다.

추가로 **보안폴더처럼 열 때 비밀번호를 요구**하는 직장 프로필 잠금(Work Profile Lock)을 지원하고, **GitHub Actions**로 APK를 자동 빌드합니다.

---

## 1. 동작 방식 (TestDPC의 "Work Profile / BYOD" 모드와 동일)

```
[개인 프로필] WorkProfileDPC 앱 실행
       │  ACTION_PROVISION_MANAGED_PROFILE 인텐트 전송
       ▼
[시스템] ① 새 직장(격리) 프로필 생성
         ② 이 앱을 직장 프로필 안으로 복사
         ③ 이 앱을 그 프로필의 Profile Owner(직장 관리자)로 지정
       ▼
[직장 프로필] WorkProfileDPC = Profile Owner
          • 직장 프로필 잠금(비밀번호) 강제  →  보안폴더처럼 열 때 비밀번호 요구
          • 격리된 직장 앱/데이터 관리
```

- **Device Owner 전혀 불필요.** `dpm set-device-owner`나 공장초기화가 필요 없습니다.
- **개인 프로필의 앱은 관리 권한이 전혀 없습니다.** 관리자 권한은 직장 프로필 안의 이 앱만 가집니다.
- 개인 데이터와 직장 데이터가 분리되고, 직장 프로필을 제거해도 개인 데이터는 그대로 유지됩니다.

---

## 2. 앱의 두 가지 화면 (실행 위치에 따라 자동 전환)

| 실행 위치 | 화면 | 설명 |
|---|---|---|
| 개인 프로필 | 프로비저닝(생성) 화면 | `ACTION_PROVISION_MANAGED_PROFILE`로 직장 프로필 생성 |
| 직장 프로필 | 관리자(Profile Owner) 화면 | 잠금 설정 · 지금 잠그기 · 관리자 해제 |

- **비밀번호 잠금 설정** → 직장 프로필 전용 비밀번호 강제(PWD_QUALITY_ALPHANUMERIC + 4자리 이상) 및 30초 비활성 시 잠금, `ACTION_SET_NEW_PASSWORD`로 즉시 비밀번호 설정 유도.
- **지금 잠그기** → `dpm.lockNow()`로 직장 프로필 즉시 잠금.
- **관리자 해제** → `dpm.removeActiveAdmin()`로 Profile Owner 자격 해제.

---

## 3. GitHub Actions 로 APK 빌드하기

이 저장소를 GitHub에 push하면 `.github/workflows/build-apk.yml`이 자동 실행되어
**`workprofile-dpc-debug`** artifact로 APK(`app-debug.apk`)를 올립니다.

1. GitHub에 새 저장소를 만들고 `git push` 합니다.
2. **Actions** 탭에서 `Build APK` 워크플로우가 빌드되는지 확인합니다.
3. **Artifacts** → `workprofile-dpc-debug` → APK 다운로드.
4. 수동으로도 실행 가능: Actions → Build APK → **Run workflow**.

> 로컬 빌드가 필요하면 Android Studio에서 이 프로젝트를 열고
> **Build → Build Bundle(s)/APK(s) → Build APK(s)** 를 실행해도 됩니다.

---

## 4. 설치 및 사용

```
adb install app-debug.apk
```

1. 개인 프로필에서 앱 실행 → **[직장 프로필 생성하기]** 탭
2. 시스템 화면에서 프로필 생성 승인 (운영체제 본체는 영향 없음)
3. 생성 완료 시 런처에 **직장(배지)** 앱 아이콘이 나타남
4. 직장 프로필의 이 앱을 열어 **[비밀번호 잠금 설정]** → 비밀번호 지정
5. 이제 직장 앱을 열 때 비밀번호(보안폴더식)를 요구

---

## 5. 프로젝트 구조

```
WorkProfileDPC/
├── .github/workflows/build-apk.yml   # GitHub Actions APK 자동 빌드
├── app/
│   ├── build.gradle                  # 앱 모듈 설정 (minSdk 24 / compileSdk 34)
│   └── src/main/
│       ├── AndroidManifest.xml       # DPC 리시버 + MainActivity
│       ├── java/com/example/workprofiledpc/
│       │   ├── MainActivity.kt         # 개인/직장 양쪽 UI + 프로비저닝/잠금 관리
│       │   └── AdminReceiver.kt        # DPC 리시버 (Profile Owner 처리)
│       └── res/
│           ├── xml/device_admin_device.xml   # 사용 정책 선언
│           ├── layout/activity_main.xml      # 개인/직장 패널
│           └── values/                      # 문자열/테마/색상
```

## 6. 참고 / 주의사항
- 이 저장소를 만든 샌드박스에는 Android SDK가 없어 **여기서는 컴파일하지 못했고**, **GitHub Actions에서 빌드**됩니다.
- Android **7.0(Nougat) 이상**에서 동작합니다. (프로필 기능 최소 버전)
- Android 11 이상에서도 TestDPC처럼 프로필 소유자(profile owner) 프로비저닝은 동작하지만, 기기 제조사/펌웨어에 따라 동의 화면·동작이 다를 수 있습니다. **에뮬레이터(AVD) 또는 초기화 가능한 테스트 기기**에서 사용하세요.
- 직장 프로필을 완전히 제거하려면 설정(개인 프로필) → 계정/계정 및 프로필 → 직장 프로필 제거(또는 이 앱의 관리자 해제 후)로 진행합니다. 개인 데이터는 유지됩니다.

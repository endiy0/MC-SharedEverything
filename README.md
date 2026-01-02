# SharedEverything

SharedEverything은 Minecraft 1.21.x용 Paper 플러그인으로, 모든 온라인 플레이어가 하나의 글로벌 인벤토리와 글로벌 도전과제 진행 상태를 공유하도록 만듭니다.

## 요구 사항
- Paper 1.21.x
- Java 21
- Gradle

## 빌드
```bash
gradle build
```
빌드 결과 JAR는 `build/libs/SharedEverything-1.0.0.jar`에 생성됩니다.

## 설치
1) JAR 파일을 서버의 `plugins` 폴더에 넣습니다.
2) 서버를 시작/재시작합니다.
3) `plugins/SharedEverything/config.yml`을 필요에 맞게 수정한 뒤 `/sharedeverything reload`를 실행합니다.

## 명령어
- `/sharedeverything reload`
- `/sharedeverything reset inventory`
- `/sharedeverything reset advancements`
- `/sharedeverything reset all`
- `/sharedeverything status`

별칭: `/se`

## 권한
- `sharedeverything.admin` (모든 명령어)

## 설정
`plugins/SharedEverything/config.yml`
```yaml
sync:
  inventory:
    enabled: true
    include_ender_chest: true
    keep_inventory_on_death: true
  advancements:
    enabled: true
    poll_interval_ticks: 100
    exclude_namespaces_or_prefixes:
      - "minecraft:recipes/"
autosave:
  interval_ticks: 600
```

## 데이터 저장
글로벌 상태는 `plugins/SharedEverything/data.yml`에 저장됩니다:
- `globalInventory` (보관함, 갑옷, 보조 손, 엔더 상자)
- `globalAdvancements` (도전과제 키 -> 달성 기준)

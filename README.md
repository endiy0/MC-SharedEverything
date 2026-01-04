# SharedEverything

Minecraft 1.21.x Paper 서버에서 인벤토리와 발전과제를 모든 플레이어(또는 팀)와 공유하는 플러그인입니다.

## 주요 기능
- 전체 서버 공유 인벤토리/발전과제
- 스코어보드 팀별 인벤토리 공유 (`teaminventory`)
- 사망 좌표 공지 (`announcedeath`)
- 사망 시 인벤토리 유지 (`keep_inventory_on_death`)
- 자동 저장 (`autosave`)

## 요구 사항
- Paper 1.21.x
- Java 21
- Gradle

## 빌드
```bash
./gradlew build
```
Windows에서는 `gradlew.bat build`를 사용합니다.

## 설치
1) `build/libs`의 JAR를 서버 `plugins` 폴더에 복사합니다.
2) 서버를 시작/재시작합니다.
3) 설정은 `plugins/sharedeverything/config.yml`에서 변경합니다.

## 명령어
기본 명령어: `/sharedeverything` (별칭: `/se`)

```text
/sharedeverything inventory <true|false>
/sharedeverything advancement <true|false>
/sharedeverything announcedeath <true|false>
/sharedeverything teaminventory <true|false>
/sharedeverything keepinventory <true|false>
/sharedeverything reset <inventory|advancements|all>
/sharedeverything reload
/sharedeverything status
```

## 권한
- `sharedeverything.admin` (기본값: op)

## 설정 (`plugins/sharedeverything/config.yml`)
```yaml
inventory: true
advancement: true
announcedeath: false
teaminventory: true
keep_inventory_on_death: false
autosave:
  interval_ticks: 600
```

설정 항목:
- `inventory`: 전체 공유 인벤토리 활성화
- `advancement`: 발전과제 공유 활성화
- `announcedeath`: 사망 좌표 공지 활성화
- `teaminventory`: 스코어보드 팀별 인벤토리 공유 활성화
- `keep_inventory_on_death`: 사망 시 인벤토리 유지 (inventory가 true일 때만 적용)
- `autosave.interval_ticks`: 자동 저장 주기 (20 ticks = 1초, 0이면 비활성화)

## 데이터 저장 위치
플러그인 데이터 폴더는 `plugins/sharedeverything`입니다.
- `inventory.yml`: 전체 공유 인벤토리
- `advancements.yml`: 공유된 발전과제 목록
- `teams/*.yml`: 팀별 인벤토리

## 동작 참고
- `teaminventory`가 활성화되면, 메인 스코어보드 팀에 속한 플레이어는 팀 인벤토리를 사용합니다.
- 팀이 없으면 전체 공유 인벤토리를 사용합니다.
- 사망 인벤토리 유지(`keep_inventory_on_death`)는 공유 인벤토리가 활성화된 경우에만 적용됩니다.

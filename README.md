# CH-DamageSkin

Paper 1.21.x 용 **데미지 스킨 플러그인**입니다. 플레이어가 몹에게 준 피해량을 리소스팩 폰트(숫자 이미지)로 몹 위에 띄웁니다.

## 요구 사항
- Paper 1.21.x / Java 21
- 리소스팩에 데미지 스킨 폰트(json) + 숫자 이미지(png)

## 설정법

1. **폰트 추가** - 리소스팩의 `assets/minecraft/font/` 에 배포된 json 파일을 넣습니다.
2. 리소스팩의 `assets/minecraft/textures/damage_skin/` 에 배포된 png 파일을 넣습니다.
3. 인게임 명령어로 데미지 스킨을 불러옵니다. `<폰트>` 는 json 파일의 `.json` 을 뺀 이름입니다.
   `<인게임에서 사용 될 이름>` 의 띄어쓰기는 `_` 로 쓰고, `&` 색코드도 쓸 수 있습니다.
   ```
   /데미지스킨관리 등록 <폰트> <인게임에서 사용 될 이름>
   ```
4. 데미지 스킨 아이템을 지급받습니다.
   ```
   /데미지스킨관리 받기 <폰트>
   ```
5. 받은 아이템을 **웅크리고 우클릭**해서 등록합니다.
6. 등록한 스킨을 적용합니다.
   ```
   /데미지스킨
   ```

## 폰트 json 형식

기본 스킨 `default.json` 예시입니다. 숫자 글자(`1234567890`)가 png 한 줄에 순서대로 들어 있고, 이 플러그인은 데미지 숫자(`0`~`9`)를 해당 폰트로 그려서 띄웁니다.

```json
{
  "providers": [
    {"type": "bitmap", "file": "damage_skin/default.png", "ascent": 1, "height": 16, "chars": ["1234567890"]}
  ]
}
```

> ⚠️ 폰트 이름이 `default` 이면 마인크래프트 기본 폰트(`minecraft:default`)를 덮어써서 **채팅·GUI 등 모든 숫자**가 데미지 스킨 이미지로 바뀝니다.
> 기본 스킨 파일 이름은 `damage_default.json` 처럼 바꾸고 `config.yml` 의 `default-skin` 도 같게 맞추는 것을 권장합니다.

## 명령어

| 명령어 | 설명 | 권한 |
| --- | --- | --- |
| `/데미지스킨` (`/damageskin`, `/ds`) | 보유한 스킨 목록 GUI, 클릭하면 적용 | 모두 |
| `/데미지스킨관리 등록 <폰트> <이름>` | 폰트를 스킨으로 등록 (같은 폰트로 다시 입력하면 이름 수정) | `chdamageskin.admin` |
| `/데미지스킨관리 받기 <폰트> [개수]` | 스킨 아이템 받기 | `chdamageskin.admin` |
| `/데미지스킨관리 삭제 <폰트>` | 스킨 삭제 (유저 보유·장착 기록도 삭제) | `chdamageskin.admin` |
| `/데미지스킨관리 목록` | 등록된 스킨 목록 | `chdamageskin.admin` |
| `/데미지스킨관리 리로드` | `config.yml` 과 스킨 목록 다시 불러오기 (`packet-mode` 포함) | `chdamageskin.admin` |

## 동작
- **표시 대상** : 플레이어(화살 등 투사체 포함)가 **몹에게 준 피해**. 최종 피해량(`getFinalDamage`)을 반올림한 정수로 표시합니다.
- **표시 방식** : 몹 위에 `TextDisplay` 숫자가 커졌다가 줄어들며 위로 올라가고 사라집니다. 기본은 때린 본인에게만 보입니다(`only-attacker`). `packet-mode` 가 켜져 있으면 서버에 엔티티를 만들지 않고 ProtocolLib 패킷으로만 전송합니다.
- **기본 스킨** : 스킨을 적용하지 않은 유저는 `default-skin` 폰트로 표시됩니다. `/데미지스킨` 의 첫 칸(기본 데미지 스킨)을 누르면 기본 스킨으로 돌아갑니다. `default-skin` 을 비워 두면 스킨을 적용한 유저에게만 표시됩니다.
- **등록과 보유** : 아이템을 웅크리고 우클릭해야 그 유저가 스킨을 보유하게 되고, `/데미지스킨` GUI 에 나타납니다.

## 요구 사항 (추가)
- (선택) **ProtocolLib 5.3 이상** : 있으면 패킷 방식으로 표시합니다. 없으면 TextDisplay 엔티티 방식으로 표시합니다.
- (선택) **ModelEngine** : 있으면 모델 몹의 높이를 기준으로 숫자를 띄웁니다.

## config.yml
| 항목 | 기본값 | 설명 |
| --- | --- | --- |
| `default-skin` | `damage_default` | 스킨 미적용 유저의 기본 폰트 (비우면 표시 안 함) |
| `packet-mode` | `true` | ProtocolLib 패킷으로 표시. 꺼져 있거나 ProtocolLib 이 없거나 전송에 실패하면 엔티티 방식 |
| `only-attacker` | `true` | true = 때린 본인에게만 보임 |
| `other-opacity` | `0.5` | 다른 유저가 볼 때의 투명도 (`only-attacker: false` 일 때) |
| `view-distance` | `24.0` | 다른 유저에게 보이는 거리(블록) |
| `actual-damage-mode` | `false` | true = 1틱 뒤 체력 감소량(실제 피해량)을 표시 |
| `offset` | `0.0` | 숫자가 뜨는 높이 보정(블록) |
| `random-offset` | `0.4` | 숫자 위치를 흩뿌리는 범위(블록) |
| `scale` | `1.5` | 숫자 기본 크기 배율 |
| `animation.*` | - | 커졌다가(`pop-scale`) → 줄어들고(`settle-scale`) → 머문 뒤(`stay-ticks`) 위로 올라가며(`rise`, `rise-ticks`) 어두워지는(`colors`) 애니메이션 |
| `crit.*` | - | 치명타 폰트(`font`) / 크기 배율(`scale-multiplier`) / 색(`colors`). 위 "치명타 표시" 참고 |
| `modelengine.enabled` / `height-scale` | `true` / `1.0` | ModelEngine 모델 높이 보정 |
| `messages.*` | - | 모든 메시지, GUI 제목·버튼, 스킨 아이템 이름·설명 (`&` 색코드, `{name}` = 스킨 이름) |

> 기존 서버는 `config.yml` 이 자동으로 갱신되지 않습니다. 새 항목을 쓰려면 `config.yml` 을 지우고 재시작하거나 위 항목을 직접 추가하세요. (없는 항목은 기본값으로 동작합니다)

## 치명타 표시
치명타 판정은 이 플러그인이 하지 않습니다. 치명타를 계산하는 스크립트에서 치명타가 터진 순간 **같은 틱 안에** 아래를 호출하면, 그 타격은 치명타 스타일로 표시됩니다. (skript-reflect)
```
import:
    com.ch.damageskin.CHDamageSkin

# 치명타로 판정된 곳에서
CHDamageSkin.markCrit({_player}, {_target})
```
- `crit.font` 에 폰트를 넣으면 치명타만 다른 폰트로, `crit.scale-multiplier` 로 크기를, `crit.colors` 로 색을 바꿀 수 있습니다.
- 표시는 0.5초 안에 피해 이벤트가 처리될 때만 유효합니다.

## 데이터 파일
- `plugins/CH-DamageSkin/skins.yml` : 등록된 스킨 (폰트 -> 이름)
- `plugins/CH-DamageSkin/players.yml` : 유저별 보유·적용 스킨

## 빌드
```
mvn package
```

# 椤圭洰姒傝

褰撳墠鏋舵瀯浠?core + target catalog 涓轰腑蹇冿細

- `api`锛欽ava API/杈圭晫濂戠害銆?- `mcmod`锛氬钩鍙版棤鍏宠繍琛屾鏋朵笌 Clojure 閫昏緫銆?- `ac`锛氬唴瀹瑰眰銆?- `platform-src/minecraft + platform-src/loader`锛氬钩鍙?target 鍏变韩 glue銆?- `platform-src/minecraft/mc-1.20.1`锛歁inecraft 1.20.1 API 閫傞厤銆?- `platform-src/loader/forge`锛欶orge loader 鍏ュ彛涓庣粦瀹氥€?- `platform-src/loader/fabric`锛欶abric loader 鍏ュ彛涓庣粦瀹氥€?- `:platform`锛氬敮涓€ Gradle 骞冲彴宸ョ▼锛屾寜 `scripts/target-gradle.ps1 <target-id>` 閫夋嫨鐩爣銆?
`platform-catalog.json` 鏄敮涓€鐩爣鐩綍锛涗笉瑕侀€氳繃鐩綍鍚嶃€佷换鍔″悕鎴栧瓧绗︿覆瑙ｆ瀽鎺ㄥ target 琛屼负銆?
## 甯哥敤鍛戒护

- 榛樿 Forge 缂栬瘧锛歚.\gradlew.bat :platform:compileClojure`
- 鎸囧畾 Forge锛歚.`\\scripts\\target-gradle.ps1 forge-1.20.1``
- 鎸囧畾 Fabric锛歚.`\\scripts\\target-gradle.ps1 fabric-1.20.1``
- 鏋舵瀯楠岃瘉锛歚.\gradlew.bat verifyCurrentPlatforms`

鏈粨搴撲笉鍖呭惈鐪熷疄 new-loader synthetic fixture target锛涙柊澧?target 蹇呴』鍏堟墿灞?catalog 涓?source component锛岃€屼笉鏄鍒舵棫骞冲彴宸ョ▼銆?

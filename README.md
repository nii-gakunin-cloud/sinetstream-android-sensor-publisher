<!--
Copyright (C) 2020-2021 National Institute of Informatics

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

[English](README.en.md)

## 概要

本アプリケーションは、Pub/Subメッセージングモデルにおける送受信
クライアントのうち`Publisher`機能(`Writer`)を実装する。  
大抵のAndroid端末は複数の組み込みセンサーデバイスを具備している。
GUI操作によりユーザが指定したこれらのセンサーの読取値を
[SINETStreamHelperライブラリ](https://www.sinetstream.net/docs/userguide/libhelper.html)
よりJSON形式で受け取り、これをAndroid版の
[SINETStreamライブラリ](https://www.sinetstream.net/docs/userguide/android.html)
を介して`SINETStreamメッセージ`として対向`Broker`に送信する。  
一方のバックエンド側では、データ可視化などのため受信メッセージ
内容の蓄積や解析といった後処理が実施される。

```
     Android Application
    +---------------------------------+
    |  +--------+ JSON +-----------+  |
    |  | Writer | <----| libhelper |  |
    |  +--------+      +-----------+  |
    |      |                A         |
    |      V                | Raw data|
    |  +-------------+  +--------+    |
    |  | SINETStream |  | Sensor |    |
    |  | for Android |  | Devices|    |                    Backend
    |  +-------------+  +--------+    |                    System
    +------|--------------------------+                   +----------+
           |                             (         )      |          |
           |                             (         )      |          |
           |                             ( Network )      | +------+ |
           +---------------------------->(         )----->| |Broker| |
               [message]                 (         )      | +------+ |
                                                          +----------+
```

## 本アプリケーションで扱うデータ
### Android端末が具備するセンサー情報
基本的にはAndroid開発者文書で記述されたセンサー種別の全てを操作対象とする。
[センサーの概要](https://developer.android.com/guide/topics/sensors/sensors_overview?hl=ja)
ただし、実際に得られるセンサー種別は動作環境（ハードウェアの実装状況やAndroidのシステムバージョン）に依存する。
また生体情報などプライバシーに関わるセンサー種別は実行時権限の承認を必要とする。
本アプリケーションの主画面（`Main`）では、当該Android端末で利用可能なセンサー種別
（実行時権限が承認されなかったものは除外）が一覧表示される。

### 端末の位置情報
本アプリケーションの設定画面（`Settings -> Sensors -> Location`）操作にて、
Android端末の位置をJSON出力の「デバイス情報」として付加できる（既定値：無効）。
本アプリケーションでは位置情報の取得源として、以下の3種類を用意する。

* [GPS](https://developer.android.com/reference/android/location/LocationManager#GPS_PROVIDER)
* [FUSED](https://developers.google.com/location-context/fused-location-provider)
* 手動設定（固定値）

なお、事前の適切なシステム設定および本アプリケーションの実行時権限の承認が必要となる。

### データ通信網の受信電波強度
Android端末のネットワーク接続方法の一つとして携帯電話網がある。
本アプリケーションの設定画面（`Settings -> Sensors -> Cellular`）操作にて、
Android端末が接続中の携帯電話網の無線種別（4G, 5Gなど）と共に、受信電波強度を
JSON出力の「デバイス情報」として 付加できる（既定値：無効）。
なお、事前の適切なシステム設定および本アプリケーションの実行時権限の承認が必要となる。

### ユーザ情報
本アプリケーションは`Pub/Subメッセージングモデル`の送信側（`Writer`）として動作し、
対向ブローカに対して`トピック`を指定してメッセージを送信する。
ブローカ側では`トピック`単位で扱うため、複数の`Writer`が`トピック`を共有する場合に
個々の`Writer`を何らかの手段で識別したい場合があるかもしれない。
本アプリケーションの設定画面（`Settings -> User Data`）操作にて、
`Writer`識別情報をJSON出力の「ユーザ情報」として付加できる（既定値：無効）。


## 動作環境

* Android 8.0 (APIレベル26) 以上
* 対向`Broker`機能が稼働するサーバ
* 上記サーバへのIP疎通の確保


## 使用例

別紙
[Android版クイックスタートガイド](https://www.sinetstream.net/docs/tutorial-android/)
のうち、`チュートリアル - STEP2: センサ情報収集アプリの実行 (sinetstream-android-sensor-publisher)`
の項を参照のこと。


## 既知の問題
### 1) 古すぎる「Paho Mqtt Android」ライブラリ
`Android 12`以降では、ブローカと接続直後に
[Paho Mqtt Android Service](https://github.com/eclipse/paho.mqtt.android)
ライブラリ内部で「例外エラー」が発生する。
これは同ライブラリが数年にわたって維持管理されておらず、最近のシステムAPIの使用方法の
変更に対応できていないためである。
暫定対処として、手元で修正した代替ライブラリ`pahomqttandroid-bugfix`を使用する。

### 2) 主画面におけるセンサー種別の一括選択/解除
操作の利便性のためにセンサー種別を一括して選択／解除できるボタンを用意した。
しかしながらセンサー種別のリストを上下にスクロール操作すると描画制御が乱れる場合があるらしい。
内部処理としては狙った通りに動作しているため、描画の齟齬の解消は継続課題としたい。


## ライセンス

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)


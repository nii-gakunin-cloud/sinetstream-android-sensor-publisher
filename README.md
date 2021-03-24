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
--->

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

## 動作環境

* Android 8.0 (APIレベル26) 以上
* 対向`Broker`機能が稼働するサーバ
* 上記サーバへのIP疎通の確保


## 使用例

別紙
[Android版クイックスタートガイド](https://www.sinetstream.net/docs/tutorial-android/)
のうち、`チュートリアル - STEP2: センサ情報収集アプリの実行 (sinetstream-android-sensor-publisher)`
の項を参照のこと。


## ライセンス

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)


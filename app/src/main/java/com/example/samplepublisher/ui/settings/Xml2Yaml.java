/*
 * Copyright (c) 2021 National Institute of Informatics
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.example.samplepublisher.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.example.samplepublisher.R;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import jp.ad.sinet.stream.android.api.ApiKeys;

public class Xml2Yaml {
    static final String TAG = Xml2Yaml.class.getSimpleName();

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public Xml2Yaml(@NonNull Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setupInfo() {
        Map<String, Object> serviceInfo = new TreeMap<>();
        Map<String, Object> serviceMap = new TreeMap<>();

        setFixedValues(serviceInfo);
        setService(serviceInfo);
        setBrokers(serviceInfo);
        setMqtt(serviceInfo);
        setUserAuth(serviceInfo);
        setTls(serviceInfo);
        setCrypto(serviceInfo);

        String serviceName = getServiceName();
        if (serviceName != null && !serviceName.isEmpty()) {
            serviceMap.put(serviceName, serviceInfo);
        } else {
            Log.w(TAG, "ServiceName not set?");
            return;
        }

        final String filename = ApiKeys.CONFIG_FILENAME;
        final String path = mContext.getFilesDir() + "/" + filename;
        dumpYaml(serviceMap, path);
    }

    private void dumpYaml(
            @NonNull Map<String, Object> serviceMap, @NonNull String path) {
        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(path);
        } catch (FileNotFoundException | SecurityException e) {
            Log.e(TAG, "path(" + path + "): " + e.getMessage());
            return;
        }

        Yaml yaml = new Yaml();
        Log.d("YAML", yaml.dumpAs(serviceMap, Tag.MAP, DumperOptions.FlowStyle.BLOCK));
        try {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            yaml.dump(serviceMap, writer);
            writer.close();
        } catch (Exception ex) {
            Log.e(TAG, "yaml.dump: " + ex.getMessage());
        }
    }

    private void setFixedValues(@NonNull Map<String, Object> serviceInfo) {
        serviceInfo.put("type", "mqtt");
        serviceInfo.put("value_type", "text");
    }

    private void setService(@NonNull Map<String, Object> serviceInfo) {
        ArrayList<String> topics = new ArrayList<>();
        final String[] strValKeys = {
                mContext.getString(R.string.pref_key_service_topics),
                mContext.getString(R.string.pref_key_service_consistency),
        };
        String strval, key;
        final int offset = "service_".length();

        for (String strValKey : strValKeys) {
            key = strValKey;
            strval = mSharedPreferences.getString(key, null);
            if (key.equals(mContext.getString(R.string.pref_key_service_topics))) {
                if (strval != null && !strval.isEmpty()) {
                    topics.add(strval);
                }
                if (topics.size() > 0) {
                    serviceInfo.put(key.substring(offset), topics);
                }
            } else {
                if (strval != null && !strval.isEmpty()) {
                    serviceInfo.put(key.substring(offset), strval);
                }
            }
        }
    }

    @Nullable
    private String getServiceName() {
        String key = mContext.getString(R.string.pref_key_service_name);
        return mSharedPreferences.getString(key, null);
    }

    private void setBrokers(@NonNull Map<String, Object> serviceInfo) {
        ArrayList<String> brokers = new ArrayList<>();
        String key;

        key = mContext.getString(R.string.pref_key_broker_listen_port);
        String probe = mSharedPreferences.getString(key, null);
        int port = 0;
        if (probe != null && !probe.isEmpty()) {
            try {
                port = Integer.parseInt(probe);
            } catch (NumberFormatException e) {
                Log.w(TAG, "port(" + probe + "):" + e.getMessage());
                return;
            }
        }

        key = mContext.getString(R.string.pref_key_broker_network_address);
        String addr = mSharedPreferences.getString(key, null);
        if (addr != null && !addr.isEmpty() && (port > 0)) {
            brokers.add(addr + ":" + port);

        }
        if (brokers.size() > 0) {
            serviceInfo.put("brokers", brokers);
        }
    }

    private void setMqtt(@NonNull Map<String, Object> serviceInfo) {
        final String[] strValKeys = {
                mContext.getString(R.string.pref_key_mqtt_mqtt_version),
                mContext.getString(R.string.pref_key_mqtt_transport),
        };
        final String[] intValKeys = {
                mContext.getString(R.string.pref_key_mqtt_qos),
        };
        final String[] boolValKeys = {
                mContext.getString(R.string.pref_key_mqtt_clean_session),
                mContext.getString(R.string.pref_key_mqtt_retain),
        };
        String strval, key;
        int intval;
        boolean boolval;
        final int offset = "mqtt_".length();

        for (String strValKey : strValKeys) {
            key = strValKey;
            strval = mSharedPreferences.getString(key, null);
            if (strval != null && !strval.isEmpty()) {
                serviceInfo.put(key.substring(offset), strval);
            }
        }

        for (String intValKey : intValKeys) {
            key = intValKey;
            strval = mSharedPreferences.getString(key, null);
            if (strval != null && !strval.isEmpty()) {
                try {
                    intval = Integer.parseInt(strval);
                } catch (NumberFormatException e) {
                    Log.w(TAG, key.substring(offset) + "(" + strval + "):" + e.getMessage());
                    return;
                }
                serviceInfo.put(key.substring(offset), intval);
            }
        }

        for (String boolValKey : boolValKeys) {
            key = boolValKey;
            try {
                boolval = mSharedPreferences.getBoolean(key, false);
            } catch (ClassCastException e) {
                Log.w(TAG, key.substring(offset) + ":" + e.getMessage());
                return;
            }
            serviceInfo.put(key.substring(offset), boolval);
        }

        setMqttConnect(serviceInfo);
        setMqttInFlight(serviceInfo);
        setMqttDebug(serviceInfo);
    }

    private void setMqttConnect(@NonNull Map<String, Object> serviceInfo) {
        Map<String, Object> map = new TreeMap<>();
        final String[] intValKeys = {
                mContext.getString(R.string.pref_key_mqtt_connect_keepalive),
                mContext.getString(R.string.pref_key_mqtt_connect_connection_timeout),
        };
        final String[] boolValKeys = {
                mContext.getString(R.string.pref_key_mqtt_connect_automatic_reconnect),
        };
        String strval, key;
        int intval;
        boolean boolval;
        final int offset = "mqtt_connect_".length();

        for (String intValKey : intValKeys) {
            key = intValKey;
            strval = mSharedPreferences.getString(key, null);
            if (strval != null && !strval.isEmpty()) {
                try {
                    intval = Integer.parseInt(strval);
                } catch (NumberFormatException e) {
                    Log.w(TAG, key.substring(offset) + "(" + strval + "):" + e.getMessage());
                    return;
                }
                map.put(key.substring(offset), intval);
            }
        }

        for (String boolValKey : boolValKeys) {
            key = boolValKey;
            try {
                boolval = mSharedPreferences.getBoolean(key, false);
            } catch (ClassCastException e) {
                Log.w(TAG, key.substring(offset) + ":" + e.getMessage());
                return;
            }
            map.put(key.substring(offset), boolval);

            if (key.equals(mContext.getString(
                    R.string.pref_key_mqtt_connect_automatic_reconnect))) {
                if (boolval) {
                    setMqttReconnect(serviceInfo);
                }
            }
        }

        if (map.size() > 0) {
            serviceInfo.put("connect", map);
        }
    }

    private void setMqttReconnect(@NonNull Map<String, Object> serviceInfo) {
        Map<String, Object> map = new TreeMap<>();
        final String[] intValKeys = {
                mContext.getString(R.string.pref_key_mqtt_reconnect_max_delay),
        };
        String strval, key;
        int intval;
        final int offset = "mqtt_reconnect_".length();

        for (String intValKey : intValKeys) {
            key = intValKey;
            strval = mSharedPreferences.getString(key, null);
            if (strval != null && !strval.isEmpty()) {
                try {
                    intval = Integer.parseInt(strval);
                } catch (NumberFormatException e) {
                    Log.w(TAG, key.substring(offset) + "(" + strval + "):" + e.getMessage());
                    return;
                }
                if (intval > 0) {
                    map.put(key.substring(offset), intval);
                }
                break;
            }
        }

        if (map.size() > 0) {
            serviceInfo.put("reconnect_delay_set", map);
        }
    }

    private void setMqttInFlight(@NonNull Map<String, Object> serviceInfo) {
        Map<String, Object> map = new TreeMap<>();
        final String[] intValKeys = {
                mContext.getString(R.string.pref_key_mqtt_inflight_inflight),
        };
        final String[] boolValKeys = {
                mContext.getString(R.string.pref_key_toggle_mqtt_inflight),
        };
        String strval, key;
        int intval;
        boolean boolval;
        final int offset = "mqtt_inflight_".length();

        for (String boolValKey : boolValKeys) {
            key = boolValKey;
            try {
                boolval = mSharedPreferences.getBoolean(key, false);
            } catch (ClassCastException e) {
                Log.w(TAG, key.substring(offset) + ":" + e.getMessage());
                return;
            }
            if (!boolval) {
                /* Not set, skip this category */
                return;
            }
            break;
        }

        for (String intValKey : intValKeys) {
            key = intValKey;
            strval = mSharedPreferences.getString(key, null);
            if (strval != null && !strval.isEmpty()) {
                try {
                    intval = Integer.parseInt(strval);
                } catch (NumberFormatException e) {
                    Log.w(TAG, key.substring(offset) + "(" + strval + "):" + e.getMessage());
                    return;
                }
                map.put(key.substring(offset), intval);
                break;
            }
        }

        if (map.size() > 0) {
            key = boolValKeys[0];
            serviceInfo.put(key.substring(offset), map);
        }
    }

    private void setMqttDebug(@NonNull Map<String, Object> serviceInfo) {
        String key = mContext.getString(R.string.pref_key_toggle_mqtt_debug);
        boolean enableMqttDebug;
        final int offset = "mqtt_debug_".length();
        try {
            enableMqttDebug = mSharedPreferences.getBoolean(key, false);
        } catch (ClassCastException e) {
            Log.w(TAG, key + ":" + e.getMessage());
            return;
        }
        serviceInfo.put(key.substring(offset), enableMqttDebug);
    }

    private void setUserAuth(@NonNull Map<String, Object> serviceInfo) {
        String key = mContext.getString(R.string.pref_key_toggle_userauth);
        boolean enableUserAuth;
        try {
            enableUserAuth = mSharedPreferences.getBoolean(key, false);
        } catch (ClassCastException e) {
            Log.w(TAG, key + ":" + e.getMessage());
            return;
        }

        if (enableUserAuth) {
            Map<String, Object> map = new TreeMap<>();
            String[] strValKeys = {
                    mContext.getString(R.string.pref_key_userauth_username),
                    mContext.getString(R.string.pref_key_userauth_password),
            };
            String strval;
            final int offset = "userauth_".length();

            for (String strValKey : strValKeys) {
                key = strValKey;
                strval = mSharedPreferences.getString(key, null);
                if (strval != null && !strval.isEmpty()) {
                    map.put(key.substring(offset), strval);
                }
            }

            if (map.size() > 0) {
                serviceInfo.put("username_pw_set", map);
            }
        }
    }

    private void setTls(@NonNull Map<String, Object> serviceInfo) {
        String key = mContext.getString(R.string.pref_key_toggle_tls);
        boolean enableTls;
        try {
            enableTls = mSharedPreferences.getBoolean(key, false);
        } catch (ClassCastException e) {
            Log.w(TAG, key + ":" + e.getMessage());
            return;
        }

        if (enableTls) {
            Map<String, Object> map = new TreeMap<>();
            final String[] strValKeys = {
                    mContext.getString(R.string.pref_key_tls_protocol),
                    /* OBSOLETED
                    mContext.getString(R.string.pref_key_tls_ca_certs),
                    mContext.getString(R.string.pref_key_tls_certfile),
                    mContext.getString(R.string.pref_key_tls_keyfilePassword),
                     */
            };
            String strval;
            final String[] boolValKeys = {
                    mContext.getString(R.string.pref_key_tls_server_certs),
                    mContext.getString(R.string.pref_key_tls_client_certs),
                    mContext.getString(R.string.pref_key_tls_check_hostname),
            };
            boolean boolval;
            final int offset = "tls_".length();

            for (String strValKey : strValKeys) {
                key = strValKey;
                strval = mSharedPreferences.getString(key, null);
                if (strval != null && !strval.isEmpty()) {
                    map.put(key.substring(offset), strval);
                }
            }

            for (String boolValKey : boolValKeys) {
                key = boolValKey;
                try {
                    boolval = mSharedPreferences.getBoolean(key, false);
                } catch (ClassCastException e) {
                    Log.w(TAG, key.substring(offset) + ":" + e.getMessage());
                    return;
                }
                map.put(key.substring(offset), boolval);
            }

            if (map.size() > 0) {
                serviceInfo.put("tls", map);
            }
        }
    }

    private void setCrypto(@NonNull Map<String, Object> serviceInfo) {
        String key = mContext.getString(R.string.pref_key_toggle_crypto);
        boolean enableCrypto;
        try {
            enableCrypto = mSharedPreferences.getBoolean(key, false);
        } catch (ClassCastException e) {
            Log.w(TAG, key + ":" + e.getMessage());
            return;
        }
        serviceInfo.put("data_encryption", enableCrypto);

        if (enableCrypto) {
            Map<String, Object> map = new TreeMap<>();
            final String[] strValKeys = {
                    mContext.getString(R.string.pref_key_crypto_algorithm),
                    mContext.getString(R.string.pref_key_crypto_mode),
                    mContext.getString(R.string.pref_key_crypto_padding),
            };
            final String[] strValKeys2 = {
                    mContext.getString(R.string.pref_key_crypto_kdf_password),
            };
            final String[] intValKeys = {
                    mContext.getString(R.string.pref_key_crypto_key_length),
            };
            String strval;
            int intval;
            final int offset = "crypto_".length();
            final int offset2 = "crypto_kdf_".length();

            for (String strValKey : strValKeys) {
                key = strValKey;
                strval = mSharedPreferences.getString(key, null);
                if (strval != null && !strval.isEmpty()) {
                    map.put(key.substring(offset), strval);
                }
            }

            /* Special Handling */
            for (String strValKey2 : strValKeys2) {
                key = strValKey2;
                strval = mSharedPreferences.getString(key, null);
                if (strval != null && !strval.isEmpty()) {
                    map.put(key.substring(offset2), strval);
                }
            }

            for (String intValKey : intValKeys) {
                key = intValKey;
                strval = mSharedPreferences.getString(key, null);
                if (strval != null && !strval.isEmpty()) {
                    try {
                        intval = Integer.parseInt(strval);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, key.substring(offset) + "(" + strval + "):" + e.getMessage());
                        return;
                    }
                    map.put(key.substring(offset), intval);
                }
            }

            setCryptoKeyDerivationFunction(map);
            if (map.size() > 0) {
                serviceInfo.put("crypto", map);
            }
        }
    }

    private void setCryptoKeyDerivationFunction(@NonNull Map<String, Object> crypto) {
        Map<String, Object> map = new TreeMap<>();
        final String[] strValKeys = {
                mContext.getString(R.string.pref_key_crypto_kdf_algorithm),
        };
        final String[] intValKeys = {
                mContext.getString(R.string.pref_key_crypto_kdf_salt_bytes),
                mContext.getString(R.string.pref_key_crypto_kdf_iteration),
        };
        String strval, key;
        int intval;
        final int offset = "crypto_kdf_".length();

        for (String strValKey : strValKeys) {
            key = strValKey;
            strval = mSharedPreferences.getString(key, null);
            if (strval != null && !strval.isEmpty()) {
                map.put(key.substring(offset), strval);
            }
        }

        for (String intValKey : intValKeys) {
            key = intValKey;
            strval = mSharedPreferences.getString(key, null);
            if (strval != null && !strval.isEmpty()) {
                try {
                    intval = Integer.parseInt(strval);
                } catch (NumberFormatException e) {
                    Log.w(TAG, key.substring(offset) + "(" + strval + "):" + e.getMessage());
                    return;
                }
                map.put(key.substring(offset), intval);
            }
        }

        if (map.size() > 0) {
            crypto.put("key_derivation", map);
        }
    }

    private void setCryptoDebug(@NonNull Map<String, Object> serviceInfo) {
        String key = mContext.getString(R.string.pref_key_toggle_crypto_debug);
        boolean enableMqttDebug;
        final int offset = "crypto_debug_".length();
        try {
            enableMqttDebug = mSharedPreferences.getBoolean(key, false);
        } catch (ClassCastException e) {
            Log.w(TAG, key + ":" + e.getMessage());
            return;
        }
        serviceInfo.put(key.substring(offset), enableMqttDebug);
    }
}

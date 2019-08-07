/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dom.rustam.devices_java;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;
import android.widget.Toast;

public class NsdHelper {

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    ClientActivity activity;
    NetworkService service;

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final String TAG = "NsdHelper";
    public String mServiceName = "Devices";

    public int servicePort = 7777;

    public String serverHost;

    NsdServiceInfo mService;

    // Дополнительно
    public String discoveredServices = "";
    public String uiMessage = "";
    public  String address = "";

    public NsdHelper(Context context, NetworkService service) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.service = service;
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();

        //mNsdManager.init(mContext.getMainLooper(), this);

    }

    // Обнаружение в сети
    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success" + service);
                discoveredServices += service.getServiceName() + " \n";
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)){
                    mNsdManager.resolveService(service, mResolveListener);
                    stopDiscovery();
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    mService = null;
                }
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);        
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    // Подключение к сервису
    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                mService = serviceInfo;
                address = serviceInfo.getHost().toString().substring(1);
                service.address = address;

                Toast.makeText(mContext, "Подключено к " + serviceInfo.getHost().toString().substring(1), Toast.LENGTH_SHORT).show();
                uiMessage = "Клиент";
                // Если устройство с  режима сервера переходит на клиент
                if (service.status == NetworkService.STATUS_SERVER) {
                    service.onChangeConnectionMode();
                }
                else {
                    service.status = NetworkService.STATUS_CLIENT;
                    service.updateLogo(uiMessage);
                    service.sendToServer(Constants.PING);
                }
            }
        };
    }

    // Регистрация сервиса
    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                int i = 5;
                if ( !mServiceName.equals(nsdServiceInfo.getServiceName()) ) {
                    tearDown(); // Если уже есть такая сеть, то не регистрируем вторую
                    discoverServices(); // Запускаем обнаружение сетей
                }
                else {
                    Toast.makeText(mContext, "Сервис " + nsdServiceInfo.getServiceName() + " запущен", Toast.LENGTH_SHORT).show();
                    uiMessage = "Сервер";
                    service.updateLogo(uiMessage);
                    service.startServer();
                    connectToSelf(); // Подключаемся к самому себе
                }

            }
            
            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.d(TAG, "onRegistrationFailed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
            }
            
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }
            
        };
    }

    // Подключаемся к локальному серверу
    public void connectToSelf() {
        address = "127.0.0.1";
        service.address = address;
        service.sendToServer(Constants.PING);
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        
    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }
    
    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }
    
    public void tearDown() {
        try {
            mNsdManager.unregisterService(mRegistrationListener);
        }catch(Exception e) {}
    }
}

package com.futu.openapi.sample;

import com.futu.openapi.pb.*;
import com.futu.openapi.*;
import com.futu.openapi.pb.QotCommon.PlateInfo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class Config {

    static long userID = 10000; //牛牛号
    static long trdAcc = 0L; //业务账号，每个市场都有独立的业务账号，可以通过getAccList获取到。
    static String unlockTradePwdMd5 = MD5Util.calcMD5("123456");  //解锁交易密码的md5
    static String opendIP = "127.0.0.1";
    static int opendPort = 11111;
    static String rsaKeyFilePath = "";  //RSA私钥文件路径，用于加密和OpenD的连接。
}

//行情示例
class TestQot implements FTSPI_Qot, FTSPI_Conn {

    FTAPI_Conn_Qot qot = new FTAPI_Conn_Qot();

    public TestQot() {
        qot.setClientInfo("javaclient", 1);  //设置客户端信息
        qot.setConnSpi(this);  //设置连接回调
        qot.setQotSpi(this);   //设置行情回调
    }

    //连接OpenD
    public void start(boolean isEnableEncrypt) {
        if (isEnableEncrypt) {
            String rsaKey = null;
            try {
                byte[] buf = java.nio.file.Files.readAllBytes(Paths.get("c:\\rsa1024"));
                rsaKey = new String(buf, Charset.forName("UTF-8"));
                qot.setRSAPrivateKey(rsaKey);
            } catch (IOException e) {

            }
        }

        qot.initConnect(Config.opendIP, (short) Config.opendPort, isEnableEncrypt);
    }

    //获取全局状态
    public void getGlobalState() {
        GetGlobalState.Request req = GetGlobalState.Request.newBuilder().setC2S(
                GetGlobalState.C2S.newBuilder().setUserID(Config.userID)
        ).build();
        int seqNo = qot.getGlobalState(req);
        System.out.printf("Send GetGlobalState: %d\n", seqNo);
    }

    //测试订阅行情
    void sub() {
        QotCommon.Security sec = QotCommon.Security.newBuilder().setCode("600118")
                .setMarket(QotCommon.QotMarket.QotMarket_CNSH_Security_VALUE)   // 沪股
                .build();
        QotSub.C2S c2s = QotSub.C2S.newBuilder().addSecurityList(sec)
                .addSubTypeList(QotCommon.SubType.SubType_Basic_VALUE)  // 基础报价
                .setIsSubOrUnSub(true)      // ture表示订阅,false表示反订阅
                .setIsRegOrUnRegPush(true)  // 是否注册或反注册该连接上面行情的推送,该参数不指定不做注册反注册操作
                .setIsFirstPush(true)       // 注册后如果本地已有数据是否首推一次已存在数据,该参数不指定则默认true
                .build();
        QotSub.Request req = QotSub.Request.newBuilder().setC2S(c2s).build();
        qot.sub(req);
    }

    //测试获取订阅状态
    void getSubInfo() {
        QotGetSubInfo.C2S c2s = QotGetSubInfo.C2S.newBuilder().build();
        QotGetSubInfo.Request req = QotGetSubInfo.Request.newBuilder().setC2S(c2s).build();
        qot.getSubInfo(req);
    }

    //测试拉取历史K线数据
    void requestHistoryKL() {
        QotCommon.Security sec = QotCommon.Security.newBuilder().setCode("600118")
                .setMarket(QotCommon.QotMarket.QotMarket_CNSH_Security.getNumber())
                .build();
        QotRequestHistoryKL.C2S c2s = QotRequestHistoryKL.C2S.newBuilder()
                .setSecurity(sec)
                .setRehabType(QotCommon.RehabType.RehabType_Forward_VALUE)
                .setKlType(QotCommon.KLType.KLType_Day_VALUE)
                .setBeginTime("2015-06-01 09:30:00")
                .setEndTime("2019-06-10 16:00:00")
                .setMaxAckKLNum(1000)
                .build();
        QotRequestHistoryKL.Request req = QotRequestHistoryKL.Request.newBuilder().setC2S(c2s).build();
        qot.requestHistoryKL(req);
    }

    //测试获取基本行情，需要先订阅才能获取
    void getBasicQot() {
        QotCommon.Security sec1 = QotCommon.Security.newBuilder().setCode("600118")
                .setMarket(QotCommon.QotMarket.QotMarket_CNSH_Security.getNumber())
                .build();
        QotGetBasicQot.C2S c2s = QotGetBasicQot.C2S.newBuilder().addSecurityList(sec1).build();
        QotGetBasicQot.Request req = QotGetBasicQot.Request.newBuilder().setC2S(c2s).build();
        qot.getBasicQot(req);
    }
    
    void getPlateSet() {
        QotGetPlateSet.C2S c2s = QotGetPlateSet.C2S.newBuilder()
                .setMarket(QotCommon.QotMarket.QotMarket_CNSH_Security.getNumber())
                .setPlateSetType(QotCommon.PlateSetType.PlateSetType_All_VALUE).build();
        QotGetPlateSet.Request req = QotGetPlateSet.Request.newBuilder().setC2S(c2s).build();
        qot.getPlateSet(req);
    }
    
    void getPlateSecurity() {
        QotCommon.Security sec = QotCommon.Security.newBuilder().setCode("BK0922")
                .setMarket(QotCommon.QotMarket.QotMarket_CNSH_Security.getNumber())
                .build();
        QotGetPlateSecurity.C2S c2s = QotGetPlateSecurity.C2S.newBuilder().setPlate(sec).build();
        QotGetPlateSecurity.Request req = QotGetPlateSecurity.Request.newBuilder().setC2S(c2s).build();
        qot.getPlateSecurity(req);
    }

    //与OpenD连接和初始化完成，可以进行各种业务请求。如果ret为false，表示失败，desc中有错误信息
    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        System.out.printf("Qot onInitConnect: ret=%b desc=%s connID=%d\n", errCode, desc, client.getConnectID());
        if (errCode != 0) {
            return;
        }

//        InitConnect成功返回才能继续后面的请求
//        this.getGlobalState();
//        this.sub();
//        this.getSubInfo();
//        this.requestHistoryKL();
//        this.getBasicQot();
//        this.getPlateSet();
        this.getPlateSecurity();
        
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        System.out.printf("Qot onDisConnect: %d\n", errCode);
    }

    @Override
    public void onReply_GetGlobalState(FTAPI_Conn client, int nSerialNo, GetGlobalState.Response rsp) {
        // @see QotCommon.QotMarketState
        System.out.printf("Reply: GetGlobalState: %d  %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onReply_Sub(FTAPI_Conn client, int nSerialNo, QotSub.Response rsp) {
        System.out.printf("Reply: Sub: %d %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onReply_GetSubInfo(FTAPI_Conn client, int nSerialNo, QotGetSubInfo.Response rsp) {
        System.out.printf("Reply: getSubInfo: %d %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onReply_GetPlateSet(FTAPI_Conn client, int nSerialNo, QotGetPlateSet.Response rsp) {
        QotGetPlateSet.S2C s2c = rsp.getS2C();
        List<PlateInfo> list = s2c.getPlateInfoListList();
        for(QotCommon.PlateInfo l : list) {
                QotCommon.Security sec = l.getPlate();
                System.out.println(l.getName() + "\t" + sec.getCode());
        }
//            System.out.printf("Reply GetPlateSet: %d  %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onReply_RequestHistoryKL(FTAPI_Conn client, int nSerialNo, QotRequestHistoryKL.Response rsp) {
        System.out.printf("Reply: RequestHistoryKL: %d %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp) {
        System.out.printf("Reply: GetBasicQot: %d %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onPush_UpdateKL(FTAPI_Conn client, QotUpdateKL.Response rsp) {
        System.out.printf("Push: KL: %s\n", rsp.toString());
    }

    @Override
    public void onPush_UpdateBasicQuote(FTAPI_Conn client, QotUpdateBasicQot.Response rsp) {
        System.out.printf("Push: QOT: %s\n", rsp.toString());
    }

    @Override
    public void onReply_GetPlateSecurity(FTAPI_Conn client, int nSerialNo, QotGetPlateSecurity.Response rsp) {
        QotGetPlateSecurity.S2C s2c = rsp.getS2C();
        System.out.println(rsp.getRetMsg());
        List<QotCommon.SecurityStaticInfo> list = s2c.getStaticInfoListList();
        for(QotCommon.SecurityStaticInfo l : list) {
            QotCommon.SecurityStaticBasic basic = l.getBasic();
            QotCommon.Security sec = basic.getSecurity();
            System.out.println(basic.getName() + "\t" + sec.getCode());
        }
//            System.out.printf("Reply GetPlateSecurity: %d  %s\n", nSerialNo, rsp.toString());
    }
    
}

class MD5Util {

    static char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    static String bytes2Hex(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : arr) {
            sb.append(hexChars[(b >>> 4) & 0xF]);
            sb.append(hexChars[b & 0xF]);
        }
        return sb.toString();
    }

    static String calcMD5(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            return bytes2Hex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}

class TestTrd implements FTSPI_Conn, FTSPI_Trd {

    FTAPI_Conn_Trd trd = new FTAPI_Conn_Trd();

    TestTrd() {
        trd.setClientInfo("javaclient", 1);  //设置客户端信息
        trd.setConnSpi(this);  //设置连接相关回调
        trd.setTrdSpi(this);   //设置交易相关回调
    }

    //连接OpenD
    void start() {
        trd.initConnect(Config.opendIP, (short) Config.opendPort, false);
    }

    //解锁交易
    void unlockTrade() {
        TrdUnlockTrade.C2S c2s = TrdUnlockTrade.C2S.newBuilder()
                .setUnlock(true)
                .setPwdMD5(Config.unlockTradePwdMd5)
                .build();
        TrdUnlockTrade.Request req = TrdUnlockTrade.Request.newBuilder().setC2S(c2s).build();
        trd.unlockTrade(req);
    }

    //获取交易账号列表
    void getAccList() {
        TrdGetAccList.C2S c2s = TrdGetAccList.C2S.newBuilder().setUserID(Config.userID).build();
        TrdGetAccList.Request req = TrdGetAccList.Request.newBuilder().setC2S(c2s).build();
        trd.getAccList(req);
    }

    //下单
    void placeOrder() {
        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setAccID(Config.trdAcc)
                .setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
                .build();
        TrdPlaceOrder.C2S c2s = TrdPlaceOrder.C2S.newBuilder()
                .setPacketID(trd.nextPacketID())
                .setHeader(header)
                .setTrdSide(TrdCommon.TrdSide.TrdSide_Buy_VALUE)
                .setOrderType(TrdCommon.OrderType.OrderType_Normal_VALUE)
                .setCode("01810")
                .setQty(1000)
                .setPrice(9.95)
                .setAdjustPrice(true)
                .setSecMarket(TrdCommon.TrdSecMarket.TrdSecMarket_HK_VALUE)
                .build();
        TrdPlaceOrder.Request req = TrdPlaceOrder.Request.newBuilder().setC2S(c2s).build();
        trd.placeOrder(req);
    }

    //订阅交易推送，否则不会收到订单相关通知。
    void subAccPush() {
        TrdSubAccPush.C2S c2s = TrdSubAccPush.C2S.newBuilder().addAccIDList(Config.trdAcc).build();
        TrdSubAccPush.Request req = TrdSubAccPush.Request.newBuilder().setC2S(c2s).build();
        trd.subAccPush(req);
    }

    //与OpenD连接和初始化完成，可以进行各种业务请求。如果ret为false，表示失败，desc中有错误信息
    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        System.out.printf("Trd onInitConnect: ret=%b desc=%s connID=%d\n", errCode, desc, client.getConnectID());
        if (errCode != 0) {
            return;
        }

//        getAccList();
        unlockTrade();
    }

    //连接断开
    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        System.out.printf("Trd onDisConnect: %d\n", errCode);
    }

    @Override
    public void onReply_GetAccList(FTAPI_Conn client, int nSerialNo, TrdGetAccList.Response rsp) {
        System.out.printf("Reply GetAccList: %d %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onReply_UnlockTrade(FTAPI_Conn client, int nSerialNo, TrdUnlockTrade.Response rsp) {
        System.out.printf("Reply UnlockTrade: %d %s\n", nSerialNo, rsp.toString());
        subAccPush();
        placeOrder();
    }

    @Override
    public void onReply_SubAccPush(FTAPI_Conn client, int nSerialNo, TrdSubAccPush.Response rsp) {
        System.out.printf("Reply SubAccPush: %d %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onReply_PlaceOrder(FTAPI_Conn client, int nSerialNo, TrdPlaceOrder.Response rsp) {
        System.out.printf("Reply PlaceOrder: %d %s\n", nSerialNo, rsp.toString());
    }

    @Override
    public void onPush_UpdateOrder(FTAPI_Conn client, TrdUpdateOrder.Response rsp) {
        System.out.printf("Push UpdateOrder: %s\n", rsp.toString());
    }

    @Override
    public void onPush_UpdateOrderFill(FTAPI_Conn client, TrdUpdateOrderFill.Response rsp) {
        System.out.printf("Push UpdateOrderFill: %s\n", rsp.toString());
    }
    
    public static String toOct(String s)
    {
        String result = "";
        byte[] bytes = s.getBytes();
        for (byte b : bytes)
        {
            int b1 = b;
            if (b1 < 0) b1 = 256 + b1;
            result += "\\" + (b1 / 64) % 8 +  "" + (b1 / 8) % 8 + "" + b1 % 8;
        }
        return result;
    }

    public static String getOct(String s) throws UnsupportedEncodingException
    {
        String[] as = s.split("\\\\");
        byte[] arr = new byte[as.length - 1];
        for (int i = 1; i < as.length; i++)
        {
            int sum = 0;
            int base = 64;
            for (char c : as[i].toCharArray())
            {
                sum += base * ((int)c - '0');
                base /= 8;
            }
            if (sum >= 128) sum = sum - 256;
            arr[i - 1] = (byte)sum;
        }
        return new String(arr,"UTF-8"); //如果还有乱码，这里编码方式你可以修改下，比如试试看unicode gbk等等
    }
}

public class Main {

    public static void main(String[] args) throws Exception {
        // write your code here
        FTAPI.init();
        TestQot qot = new TestQot();
        qot.start(false);
//        TestTrd trd = new TestTrd();
//        trd.start();

        while (true) {
            try {
                Thread.sleep(1000 * 600);
            } catch (InterruptedException exc) {

            }
        }
    }
}

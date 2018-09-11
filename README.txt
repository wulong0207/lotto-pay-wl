支付注意事项:
1、易宝支付需要设置host才可以进行支付：否则会报：交易网址存在风险，请联系商家。设置host方法如下：
  C:\Windows\System32\drivers\etc  在末尾加上：127.0.0.1      cp.2ncai.com
2、易宝的网银支付的异步回调地址要到易宝的商户后台配置。否则收不到服务端通知
    本机地址：http://roseking.iask.in:52156/lotto/v1.0/rechargeCenter/yeePay   ; 测试的地址换前面IP
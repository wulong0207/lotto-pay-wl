package com.hhly.utils;

import java.io.ByteArrayOutputStream;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

public class CodeUtil {

	private static final int WIDTH = 370;// 宽度
	private static final int HEIGHT = 370;// 高度

	/**  
	* 方法说明: 生成不带logo的二维码
	* @auth: xiongJinGang
	* @param formLink
	* @time: 2017年7月31日 下午3:29:40
	* @return: byte[] 
	*/
	public static byte[] getQrCode(String formLink) {
		ByteArrayOutputStream outStream = QRCode.from(formLink).to(ImageType.PNG).withSize(WIDTH, HEIGHT).stream();
		return outStream.toByteArray();
	}
}

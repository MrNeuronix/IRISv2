package ru.iris.common.source.vk.entities;

/**
 * Author: akorobitsyn
 * Date: 03.07.13
 * Time: 11:54
 */
public class Captcha
{

	private String captchaSid;

	private String captchaImg;

	private String captchaKey;

	public String getCaptchaSid()
	{
		return captchaSid;
	}

	public void setCaptchaSid(String captchaSid)
	{
		this.captchaSid = captchaSid;
	}

	public String getCaptchaImg()
	{
		return captchaImg;
	}

	public void setCaptchaImg(String captchaImg)
	{
		this.captchaImg = captchaImg;
	}

	public String getCaptchaKey()
	{
		return captchaKey;
	}

	public void setCaptchaKey(String captchaKey)
	{
		this.captchaKey = captchaKey;
	}

	@Override
	public String toString()
	{
		return "Captcha{" +
				"captchaSid='" + captchaSid + '\'' +
				", captchaImg='" + captchaImg + '\'' +
				", captchaKey='" + captchaKey + '\'' +
				'}';
	}
}

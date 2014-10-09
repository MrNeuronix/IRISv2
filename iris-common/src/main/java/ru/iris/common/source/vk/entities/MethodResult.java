package ru.iris.common.source.vk.entities;

/**
 * Author: akorobitsyn
 * Date: 09.07.13
 * Time: 19:21
 */
public class MethodResult
{

	private boolean success;
	private long errorCode;
	private String message;
	private Captcha captcha;

	public boolean isSuccess()
	{
		return success;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	public long getErrorCode()
	{
		return errorCode;
	}

	public void setErrorCode(long errorCode)
	{
		this.errorCode = errorCode;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Captcha getCaptcha()
	{
		return captcha;
	}

	public void setCaptcha(Captcha captcha)
	{
		this.captcha = captcha;
	}

	@Override
	public String toString()
	{
		return "MethodResult{" +
				"success=" + success +
				", errorCode=" + errorCode +
				", message='" + message + '\'' +
				", captcha=" + captcha +
				'}';
	}
}

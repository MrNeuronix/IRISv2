package ru.iris.common.source.vk.entities;

/**
 * Author: akorobitsyn
 * Date: 09.07.13
 * Time: 19:25
 */
public class AddFriendResult
{

	private int result;
	private MethodResult methodResult;

	public AddFriendResult()
	{
		this.methodResult = new MethodResult();
	}

	public void setSuccess(boolean success)
	{
		this.methodResult.setSuccess(success);
	}

	public boolean isSuccess()
	{
		return methodResult.isSuccess();
	}

	public void setMessage(String message)
	{
		this.methodResult.setMessage(message);
	}

	public String getMessage()
	{
		return methodResult.getMessage();
	}

	public void setErrorCode(long errorCode)
	{
		this.methodResult.setErrorCode(errorCode);
	}

	public long getErrorCode()
	{
		return methodResult.getErrorCode();
	}

	public void setCaptcha(Captcha captcha)
	{
		this.methodResult.setCaptcha(captcha);
	}

	public Captcha getCaptcha()
	{
		return methodResult.getCaptcha();
	}

	public int getResult()
	{
		return result;
	}

	public void setResult(int result)
	{
		this.result = result;
	}

	public MethodResult getMethodResult()
	{
		return methodResult;
	}

	@Override
	public String toString()
	{
		return "AddFriendResult{" +
				"result=" + result +
				", methodResult=" + methodResult +
				'}';
	}
}

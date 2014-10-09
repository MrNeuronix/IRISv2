package ru.iris.common.source.vk.entities;

/**
 * Author: akorobitsyn
 * Date: 02.07.13
 * Time: 11:03
 */
public class WallPostResult
{
	private long postId;
	private MethodResult methodResult;

	public WallPostResult()
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

	public long getPostId()
	{
		return postId;
	}

	public void setPostId(long postId)
	{
		this.postId = postId;
	}

	public MethodResult getMethodResult()
	{
		return methodResult;
	}

	@Override
	public String toString()
	{
		return "WallPostResult{" +
				"postId=" + postId +
				", methodResult=" + methodResult +
				'}';
	}
}

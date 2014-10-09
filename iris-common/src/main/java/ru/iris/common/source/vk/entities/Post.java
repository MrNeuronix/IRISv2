package ru.iris.common.source.vk.entities;

import java.util.List;

/**
 * Author: akorobitsyn
 * Date: 02.07.13
 * Time: 10:41
 */
public class Post
{

	private String message;
	private List<String> attachments;
	private Double latitude;
	private Double longitude;
	private boolean fromGroup = false;

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public List<String> getAttachments()
	{
		return attachments;
	}

	public void setAttachments(List<String> attachments)
	{
		this.attachments = attachments;
	}

	public Double getLatitude()
	{
		return latitude;
	}

	public void setLatitude(Double latitude)
	{
		this.latitude = latitude;
	}

	public Double getLongitude()
	{
		return longitude;
	}

	public void setLongitude(Double longitude)
	{
		this.longitude = longitude;
	}

	public boolean isFromGroup()
	{
		return fromGroup;
	}

	public void setFromGroup(boolean fromGroup)
	{
		this.fromGroup = fromGroup;
	}

	@Override
	public String toString()
	{
		return "Post{" +
				"message='" + message + '\'' +
				", attachments=" + attachments +
				", latitude=" + latitude +
				", longitude=" + longitude +
				", fromGroup=" + fromGroup +
				'}';
	}
}

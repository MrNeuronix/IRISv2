package ru.iris.common.source.vk.entities;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Author: akorobitsyn
 * Date: 01.07.13
 * Time: 14:48
 */
public class Group
{

	private long gid;
	private String name;
	@JsonProperty("screen_name")
	private String screenName;
	@JsonProperty("is_closed")
	private boolean closed;
	@JsonProperty("is_admin")
	private boolean admin;
	@JsonProperty("admin_level")
	private int admin_level;
	@JsonProperty("is_member")
	private boolean member;
	private String type;
	private String photo;
	@JsonProperty("photo_medium")
	private String photoMedium;
	@JsonProperty("photo_big")
	private String photoBig;
	@JsonProperty("deactivated")
	private String deactivated;

	public long getGid()
	{
		return gid;
	}

	public void setGid(long gid)
	{
		this.gid = gid;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getScreenName()
	{
		return screenName;
	}

	public void setScreenName(String screenName)
	{
		this.screenName = screenName;
	}

	public boolean isClosed()
	{
		return closed;
	}

	public void setClosed(boolean closed)
	{
		this.closed = closed;
	}

	public boolean isAdmin()
	{
		return admin;
	}

	public void setAdmin(boolean admin)
	{
		this.admin = admin;
	}

	public int getAdmin_level()
	{
		return admin_level;
	}

	public void setAdmin_level(int admin_level)
	{
		this.admin_level = admin_level;
	}

	public boolean isMember()
	{
		return member;
	}

	public void setMember(boolean member)
	{
		this.member = member;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getPhoto()
	{
		return photo;
	}

	public void setPhoto(String photo)
	{
		this.photo = photo;
	}

	public String getPhotoMedium()
	{
		return photoMedium;
	}

	public void setPhotoMedium(String photoMedium)
	{
		this.photoMedium = photoMedium;
	}

	public String getPhotoBig()
	{
		return photoBig;
	}

	public void setPhotoBig(String photoBig)
	{
		this.photoBig = photoBig;
	}

	public String getDeactivated()
	{
		return deactivated;
	}

	public void setDeactivated(String deactivated)
	{
		this.deactivated = deactivated;
	}

	@Override
	public String toString()
	{
		return "Group{" +
				"gid=" + gid +
				", name='" + name + '\'' +
				", screenName='" + screenName + '\'' +
				", closed=" + closed +
				", admin=" + admin +
				", admin_level=" + admin_level +
				", member=" + member +
				", type='" + type + '\'' +
				", photo='" + photo + '\'' +
				", photoMedium='" + photoMedium + '\'' +
				", photoBig='" + photoBig + '\'' +
				", deactivated='" + deactivated + '\'' +
				'}';
	}
}

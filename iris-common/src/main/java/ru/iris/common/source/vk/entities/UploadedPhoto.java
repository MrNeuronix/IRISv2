package ru.iris.common.source.vk.entities;

/**
 * Author: akorobitsyn
 * Date: 08.07.13
 * Time: 13:54
 */
public class UploadedPhoto
{

	private long server;

	private String photo;

	private String hash;

	public long getServer()
	{
		return server;
	}

	public void setServer(long server)
	{
		this.server = server;
	}

	public String getPhoto()
	{
		return photo;
	}

	public void setPhoto(String photo)
	{
		this.photo = photo;
	}

	public String getHash()
	{
		return hash;
	}

	public void setHash(String hash)
	{
		this.hash = hash;
	}

	@Override
	public String toString()
	{
		return "UploadedPhoto{" +
				"server=" + server +
				", photo='" + photo + '\'' +
				", hash='" + hash + '\'' +
				'}';
	}
}

package ru.iris.common.source.vk.entities;

/**
 * Author: akorobitsyn
 * Date: 09.07.13
 * Time: 16:27
 */
public class User
{

	private long id;
	private String bdate;
	private int sex;
	private long cityId;
	private int online;
	private String firstName;
	private String lastName;

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getBdate()
	{
		return bdate;
	}

	public void setBdate(String bdate)
	{
		this.bdate = bdate;
	}

	public int getSex()
	{
		return sex;
	}

	public void setSex(int sex)
	{
		this.sex = sex;
	}

	public long getCityId()
	{
		return cityId;
	}

	public void setCityId(long cityId)
	{
		this.cityId = cityId;
	}

	public int getOnline()
	{
		return online;
	}

	public void setOnline(int online)
	{
		this.online = online;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	@Override
	public String toString()
	{
		return "User{" +
				"id=" + id +
				", bdate='" + bdate + '\'' +
				", cityId=" + cityId +
				", online=" + online +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		User user = (User) o;

		if (cityId != user.cityId)
			return false;
		if (id != user.id)
			return false;
		if (online != user.online)
			return false;
		if (sex != user.sex)
			return false;
		if (bdate != null ? !bdate.equals(user.bdate) : user.bdate != null)
			return false;
		if (firstName != null ? !firstName.equals(user.firstName) : user.firstName != null)
			return false;
		return !(lastName != null ? !lastName.equals(user.lastName) : user.lastName != null);

	}

	@Override
	public int hashCode()
	{
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (bdate != null ? bdate.hashCode() : 0);
		result = 31 * result + sex;
		result = 31 * result + (int) (cityId ^ (cityId >>> 32));
		result = 31 * result + online;
		result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
		result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
		return result;
	}
}

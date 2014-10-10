package ru.iris.common.source.vk;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import ru.iris.common.source.vk.entities.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author: akorobitsyn
 * Date: 29.07.13
 * Time: 19:20
 */
public class VKConnectorImpl implements VKConnector
{
	//getting token
	//https://oauth.vk.com/authorize?client_id=3858624&scope=groups,wall,photos,friends,status,offline&redirect_uri=http://oauth.vk.com/blank.html&display=page&response_type=token
	private final Log log = LogFactory.getLog(VKConnector.class);

	private final static String authUrl = "https://oauth.vk.com/token?grant_type=password&client_id=%d&client_secret=%s&username=%s&password=%s&v=5.25";

	private final static String groupsSearchUrl = "https://api.vk.com/method/groups.search?q=%s&offset=%d&count=%d&access_token=%s";
	private final static String groupMembersSearchUrl = "https://api.vk.com/method/groups.getMembers?group_id=%d&sort=time_desc&offset=%d&count=%d&access_token=%s";
	private final static String usersGetUrl = "https://api.vk.com/method/users.get";
	private final static String getFriendsUrl = "https://api.vk.com/method/friends.get?user_id=%d&access_token=%s";

	private final static String joinGroupUrl = "https://api.vk.com/method/groups.join?group_id=%d&access_token=%s";
	private final static String friendsAddUrl = "https://api.vk.com/method/friends.add";
	private final static String wallPostWithAttachmentsUrl = "https://api.vk.com/method/wall.post";

	//    private final static String getWallUploadServerUrl = "https://api.vk.com/method/wall.getPhotoUploadServer?access_token=%s";
	private final static String getWallUploadServerUrl = "https://api.vk.com/method/photos.getWallUploadServer?save_big=%d&access_token=%s";
	//    private final static String saveWallPhotoUrl = "https://api.vk.com/method/wall.savePost";
	private final static String saveWallPhotoUrl = "https://api.vk.com/method/photos.saveWallPhoto";

	private final static String setStatusUrl = "https://api.vk.com/method/status.set";

	private final HttpClient client;
	private ObjectMapper objectMapper = new ObjectMapper();

	public static VKConnector createInstance(HttpClient httpClient)
	{
		return new VKConnectorImpl(httpClient);
	}

	public static VKConnector createInstance()
	{
		return new VKConnectorImpl(new DefaultHttpClient());
	}

	public String getToken(int appid, String appkey, String username, String password) throws IOException
	{
		String request = String.format(authUrl, appid, appkey, username, password);
		log.debug(request);
		HttpGet httpGet = new HttpGet(request);
		HttpResponse response = client.execute(httpGet);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		JsonNode result = objectMapper.readTree(responseBody);

		if (result.get("access_token") != null)
			return result.get("access_token").asText();
		else
			return null;
	}

	public List<Group> searchGroups(String query, int count, String token) throws IOException
	{
		String request = String.format(groupsSearchUrl, URLEncoder.encode(query, "UTF-8"), 0, count, token);
		log.debug(request);
		HttpGet httpGet = new HttpGet(request);
		HttpResponse response = client.execute(httpGet);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		JsonNode resultTree = objectMapper.readTree(responseBody);
		ArrayNode arrayNode = (ArrayNode) resultTree.get("response");
		List<Group> groups = new ArrayList<>();
		for (int i = 1; i < arrayNode.size(); i++)
		{
			Group group = objectMapper.readValue(arrayNode.get(i).toString(), Group.class);
			groups.add(group);
		}

		return groups;
	}

	public List<Long> getFriends(User user, String token) throws IOException
	{
		String request = String.format(getFriendsUrl, user.getId(), token);
		log.debug(request);
		HttpGet httpGet = new HttpGet(request);
		HttpResponse response = client.execute(httpGet);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		JsonNode resultTree = objectMapper.readTree(responseBody);
		List<Long> result = new ArrayList<>();
		ArrayNode usersJsonNode = (ArrayNode) resultTree.get("response");
		for (JsonNode userJsonNode : usersJsonNode)
		{
			result.add(userJsonNode.asLong());
		}
		return result;
	}

	public List<User> getUsers(Collection<Long> userIds, String name_case, String token) throws IOException
	{
		String request = usersGetUrl;
		log.debug(request);
		HttpPost httpPost = new HttpPost(request);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);

		if (userIds != null)
		{
			String userIdsStr = "";
			for (Long userId : userIds)
			{
				if (userIdsStr != "")
				{
					userIdsStr += ",";
				}
				userIdsStr += String.valueOf(userId);
			}
			nameValuePairs.add(new BasicNameValuePair("uids", userIdsStr));
		}

		nameValuePairs.add(new BasicNameValuePair("fields", "bdate,sex,city,online"));
		nameValuePairs.add(new BasicNameValuePair("access_token", token));
		nameValuePairs.add(new BasicNameValuePair("name_case", name_case));
		log.debug(nameValuePairs);
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		HttpResponse response = client.execute(httpPost);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		JsonNode resultTree = objectMapper.readTree(responseBody);
		ArrayNode responseJsonNode = (ArrayNode) resultTree.get("response");

		List<User> result = new ArrayList<User>();
		for (JsonNode userJsonNode : responseJsonNode)
		{
			User user = new User();
			user.setId(userJsonNode.get("uid").asLong());
			user.setFirstName(userJsonNode.get("first_name").asText());
			user.setLastName(userJsonNode.get("last_name").asText());
			user.setSex(userJsonNode.get("sex").asInt());
			if (userJsonNode.get("bdate") != null)
			{
				user.setBdate(userJsonNode.get("bdate").asText());
			}
			if (userJsonNode.get("city") != null)
			{
				user.setCityId(userJsonNode.get("city").asInt());
			}
			user.setOnline(userJsonNode.get("online").asInt());
			result.add(user);
		}
		return result;
	}

	public List<Long> getGroupMembers(long groupId, int count, String token) throws IOException
	{
		String request = String.format(groupMembersSearchUrl, groupId, 0, count, token);
		log.debug(request);
		HttpGet httpGet = new HttpGet(request);
		HttpResponse response = client.execute(httpGet);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		JsonNode resultTree = objectMapper.readTree(responseBody);
		JsonNode responseJsonNode = resultTree.get("response");
		List<Long> result = new ArrayList<Long>();
		ArrayNode usersJsonNode = (ArrayNode) responseJsonNode.get("users");
		for (JsonNode userJsonNode : usersJsonNode)
		{
			result.add(userJsonNode.asLong());
		}
		return result;
	}

	public void joinGroup(long groupId, String token) throws IOException
	{
		String request = String.format(joinGroupUrl, groupId, token);
		log.debug(request);
		HttpGet httpGet = new HttpGet(request);
		HttpResponse response = client.execute(httpGet);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);
	}

	public AddFriendResult addFriend(long uid, String text, Captcha captcha, String token) throws IOException
	{
		String request = friendsAddUrl;
		log.debug(request);
		HttpPost httpPost = new HttpPost(request);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("user_id", String.valueOf(uid)));
		nameValuePairs.add(new BasicNameValuePair("text", text));
		nameValuePairs.add(new BasicNameValuePair("access_token", token));
		if (captcha != null)
		{
			nameValuePairs.add(new BasicNameValuePair("captcha_sid", captcha.getCaptchaSid()));
			nameValuePairs.add(new BasicNameValuePair("captcha_key", captcha.getCaptchaKey()));
		}
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

		HttpResponse response = client.execute(httpPost);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		return createAddFriendResult(responseBody);
	}

	public WallPostResult wallPost(Long gid, Post post, Captcha captcha, String token) throws IOException
	{
		String request = wallPostWithAttachmentsUrl;
		log.debug(request);
		HttpPost httpPost = new HttpPost(request);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		if (gid != null)
		{
			nameValuePairs.add(new BasicNameValuePair("owner_id", String.valueOf(gid)));
		}
		if (post.isFromGroup())
		{
			nameValuePairs.add(new BasicNameValuePair("from_group", "1"));
		}
		nameValuePairs.add(new BasicNameValuePair("message", post.getMessage()));
		if (post.getAttachments() != null)
		{
			nameValuePairs.add(new BasicNameValuePair("attachments", StringUtils.join(post.getAttachments(), ",")));
		}
		nameValuePairs.add(new BasicNameValuePair("access_token", token));
		if (post.getLatitude() != null && post.getLongitude() != null
				&& post.getLatitude() != 0 && post.getLongitude() != 0)
		{
			nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(post.getLatitude())));
			nameValuePairs.add(new BasicNameValuePair("long", String.valueOf(post.getLongitude())));
		}
		if (captcha != null)
		{
			nameValuePairs.add(new BasicNameValuePair("captcha_sid", captcha.getCaptchaSid()));
			nameValuePairs.add(new BasicNameValuePair("captcha_key", captcha.getCaptchaKey()));
		}

		log.debug(nameValuePairs);
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

		HttpResponse response = client.execute(httpPost);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		JsonNode jsonNode = objectMapper.readTree(responseBody);
		WallPostResult wallPostResult = new WallPostResult();
		if (jsonNode.has("error"))
		{
			wallPostResult.setSuccess(false);
			JsonNode errorNode = jsonNode.get("error");
			wallPostResult.setMessage(errorNode.get("error_msg").asText());
			wallPostResult.setErrorCode(errorNode.get("error_code").asLong());

			if (errorNode.has("captcha_sid"))
			{
				Captcha newCaptcha = new Captcha();
				newCaptcha.setCaptchaSid(errorNode.get("captcha_sid").asText());
				newCaptcha.setCaptchaImg(errorNode.get("captcha_img").asText());
				wallPostResult.setCaptcha(newCaptcha);
			}
		}
		else
		{
			wallPostResult.setSuccess(true);
			wallPostResult.setPostId(jsonNode.get("response").get("post_id").asLong());
		}
		return wallPostResult;
	}

	@Override
	public String uploadWallPhoto(String token, byte[] bytes) throws IOException, InterruptedException
	{
		String myWallUploadServer = getMyWallUploadServer(token);
		UploadedPhoto uploadedPhoto = uploadPhoto(myWallUploadServer, bytes);
		//TODO: Thread.sleep(5000) may be needed
		return saveMyWallPhoto(uploadedPhoto, token);
	}

	public AddFriendResult setStatus(String text, String token) throws IOException
	{
		String request = setStatusUrl;
		log.debug(request);
		HttpPost httpPost = new HttpPost(request);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("access_token", token));
		nameValuePairs.add(new BasicNameValuePair("text", text));
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

		HttpResponse response = client.execute(httpPost);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		return createAddFriendResult(responseBody);
	}

	private VKConnectorImpl(HttpClient httpClient)
	{
		this.client = httpClient;
	}

	private AddFriendResult createAddFriendResult(String responseBody) throws IOException
	{
		JsonNode resultTree = objectMapper.readTree(responseBody);
		AddFriendResult addFriendResult = new AddFriendResult();
		if (resultTree.has("error"))
		{
			addFriendResult.setSuccess(false);
			JsonNode errorNode = resultTree.get("error");
			addFriendResult.setMessage(errorNode.get("error_msg").asText());
			addFriendResult.setErrorCode(errorNode.get("error_code").asLong());

			if (errorNode.has("captcha_sid"))
			{
				Captcha newCaptcha = new Captcha();
				newCaptcha.setCaptchaSid(errorNode.get("captcha_sid").asText());
				newCaptcha.setCaptchaImg(errorNode.get("captcha_img").asText());
				addFriendResult.setCaptcha(newCaptcha);
			}
		}
		else
		{
			addFriendResult.setSuccess(true);
			addFriendResult.setResult(resultTree.get("response").asInt());
		}
		return addFriendResult;
	}

	private String getMyWallUploadServer(String token) throws IOException
	{
		String request = String.format(getWallUploadServerUrl, 1, token);
//        String request = String.format(getWallUploadServerUrl, token);
		log.debug(request);
		HttpGet httpGet = new HttpGet(request);
		HttpResponse response = client.execute(httpGet);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

		JsonNode responseJsonNode = objectMapper.readTree(responseBody);
		log.debug(responseBody);

		return responseJsonNode.get("response").get("upload_url").asText();
	}

	private UploadedPhoto uploadPhoto(String serverUrl, byte[] photo) throws IOException
	{
		HttpPost httppost = new HttpPost(serverUrl);
		MultipartEntity mpEntity = new MultipartEntity();

		ByteArrayBody byteArrayBody = new ByteArrayBody(photo, "photo.jpg");
		mpEntity.addPart("photo", byteArrayBody);

		httppost.setEntity(mpEntity);
		HttpResponse response = client.execute(httppost);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		return objectMapper.readValue(responseBody, UploadedPhoto.class);
	}

	private String saveMyWallPhoto(UploadedPhoto uploadedPhoto, String token) throws IOException
	{
		String request = saveWallPhotoUrl;
		log.debug(request);
		HttpPost httpPost = new HttpPost(request);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("access_token", token));
		nameValuePairs.add(new BasicNameValuePair("photo", uploadedPhoto.getPhoto()));
		nameValuePairs.add(new BasicNameValuePair("server", String.valueOf(uploadedPhoto.getServer())));
		nameValuePairs.add(new BasicNameValuePair("hash", String.valueOf(uploadedPhoto.getHash())));
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

		HttpResponse response = client.execute(httpPost);
		String responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		log.debug(responseBody);

		JsonNode responseJsonTree = objectMapper.readTree(responseBody);
		return responseJsonTree.get("response").get(0).get("id").asText();
	}

}

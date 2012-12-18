package com.mtea.vote;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;

/**
 * 主程序
 * @author macrotea@qq.com
 * @date 2012-11-25 下午11:08:53
 * @version 1.0
 * @note
 */
public class Main 
{
	private static final String ROBOOT_URL="http://192.168.0.1/apply.cgi?CMD=reboot";
	private static final String SEMS_URL="http://www.cnscn.com.cn/iotsmartcity/solution/id/xxxxxxxxxx.html";
	private static final String VOTE_URL="http://www.cnscn.com.cn/Index/vote?id=xxxxxxxxxx&votes=%s";
	
	private static final String DATE_TIME_PATTERN="yyyy-MM-dd hh:mm:ss";
	
	/**
	 * UserAgent列表
	 */
	private static List<String> userAgentList = null;
	static {
		userAgentList = new ArrayList<String>();
		userAgentList.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
		userAgentList.add("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
		userAgentList.add("Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)");
	}
	
	/**
	 * 定义分钟
	 */
	private static final long MINIUTE = 60 * 1000;
	
	/**
	 * 程序主入口
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午2:54:56
	 * @param args
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void main(String[] args) throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {
			configClient(httpclient);
			
			for (int i = 0; i < 1000; i++) {
				
				System.out.println(String.format("\n-----------第%s次投票-----------\n", (i+1)));
				
				HttpGet httpget = new HttpGet("http://192.168.0.1");
				HttpResponse response = httpclient.execute(httpget);
				EntityUtils.consume(response.getEntity());
				System.out.println(">>路由器登录认证");
				System.out.println("请求路由器登录认证响应的状态值: " + response.getStatusLine());
				System.out.println("路由器登录认证是否成功: " + (response.getStatusLine().getStatusCode() == 200));

				// 若登录成功
				if (response.getStatusLine().getStatusCode() == 200) {
					
					// 重启路由
					doRouteReboot(httpclient, ROBOOT_URL);

					Thread.sleep(MINIUTE);

					// 请求投票
					boolean isVoteSuccess = doVote(httpclient, SEMS_URL);
					if (!isVoteSuccess) {
						break;
					}

					// 睡眠 1-3分钟之间
					Thread.sleep(MINIUTE * (new Random().nextInt(2) + 1));
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
		System.out.println(String.format("程序于%s投票失败! 程序已终止!",DateUtils.formatDate(new Date(),DATE_TIME_PATTERN)));
	}

	/**
	 * 配置Client
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午12:53:08
	 * @param httpclient
	 */
	private static void configClient(DefaultHttpClient httpclient) {
		httpclient.getCredentialsProvider().setCredentials(new AuthScope("192.168.0.1", 80), new UsernamePasswordCredentials("admin", "19860316"));
	}
    
    /**
     * 路由器重启
     * @author macrotea@qq.com
     * @date 2012-11-26 上午12:43:20
     * @param httpclient
     * @throws ClientProtocolException
     * @throws IOException
     */
	private static void doRouteReboot(DefaultHttpClient httpclient,String url) throws ClientProtocolException, IOException {
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget);
		EntityUtils.consume(response.getEntity());
		System.out.println(">>重启路由器");
        System.out.println("请求路由器重启响应的状态值: "+ response.getStatusLine());
        System.out.println("路由器重启是否成功: "+ (response.getStatusLine().getStatusCode()==200));
	}
	
	/**
	 * 请求投票页面且投票
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午12:51:00
	 * @param httpclient
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private static boolean doVote(DefaultHttpClient httpclient, String url) throws ClientProtocolException, IOException {
		String hasVotedTotal = null;

		// 保存cookie
		CookieStore cookieStore = new BasicCookieStore();
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		// 获得已经投票的票数
		HttpGet httpget = new HttpGet(url);
		HttpResponse votePageResponse = httpclient.execute(httpget, localContext);
		boolean flag = votePageResponse.getStatusLine().getStatusCode() == 200;
		System.out.println(">>请求投票页面");
		System.out.println("请求投票页面响应的状态值: " + votePageResponse.getStatusLine());
		System.out.println("投票页面是否打开成功: " + (flag));
		HttpEntity votePageEntity = votePageResponse.getEntity();
		InputStream isStrem = votePageEntity.getContent();
		hasVotedTotal = Jsoup.parse(isStrem, "utf8", "").select("span#v_23").text();
		EntityUtils.consume(votePageResponse.getEntity());

		// 执行真正的投票且传递cookies,保留现场
		List<Cookie> cookies = cookieStore.getCookies();
		return execVote(httpclient, VOTE_URL, hasVotedTotal, cookies);
	}
	/**
	 * 请求投票
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午12:51:00
	 * @param httpclient
	 * @param url
	 * @param cookies 
	 * @param hasVotedTotal 
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private static boolean execVote(DefaultHttpClient httpclient, String url, String hasVotedTotal, List<Cookie> cookies) throws ClientProtocolException, IOException {
		if (hasVotedTotal == null)
			return false;

		// 获得请求投票后返回的文本值
		HttpGet httpget = new HttpGet(String.format(url, hasVotedTotal));
		// 重写Header
		overrideHeaders(cookies, httpget);
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		//返回的文本值
		//String responseText = EntityUtils.toString(entity,"utf8");
		//assertHasVoted(responseText);

		// 200 且 返回文本 是 1
		boolean flag = (response.getStatusLine().getStatusCode() == 200);// && ("1".equals(responseText)
		System.out.println(">>请求投票");
		System.out.println("请求投票响应的状态值: " + response.getStatusLine());
		System.out.println("投票是否成功: " + (flag));
		if(flag) System.err.println("你现在的票数是:  " + (Integer.parseInt(hasVotedTotal) + 1));
		EntityUtils.consume(entity);
		return flag;
	}

	/**
	 * 判断是否已经投过票了
	 * 
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午2:23:17
	 * @param responseText
	 */
	private static void assertHasVoted(String responseText) {
		boolean result = ("2".equals(responseText));
		if(result) System.out.println("你已经投过票了!");
	}

	/**
	 * 重写Header
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午2:13:49
	 * @param cookies
	 * @param httpget
	 * @throws IOException
	 */
	public static void overrideHeaders(List<Cookie> cookies, HttpGet httpget) throws IOException {
		List<Header> headerList = new ArrayList<Header>();
		headerList.add(new BasicHeader("User-Agent", getRandomUserAgent()));
		headerList.add(new BasicHeader("Referer", SEMS_URL));
		headerList.add(new BasicHeader("Cookie", getCookiesText(cookies)));
		Header[] headers = new Header[headerList.size()];
		httpget.setHeaders(headerList.toArray(headers));
	}

	/**
	 * 根据Cookie集合获得文本字符串
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午2:02:23
	 * @param cookieList
	 * @return
	 * @throws IOException
	 */
	private static String getCookiesText(List<Cookie> cookieList) throws IOException {
		StringBuilder sb = new StringBuilder();
		int size = cookieList.size();
		for (int j = 0; j < size; j++) {
			Cookie cookie = cookieList.get(j);
			sb.append(cookie.getName()).append("=").append(cookie.getValue());
			if (j != size) {
				sb.append("; ");
			}
		}
		return sb.toString();
	}
	
	/**
	 * 获得随机的UserAgent
	 * @author macrotea@qq.com
	 * @date 2012-11-26 上午2:08:32
	 * @return
	 */
	private static String getRandomUserAgent(){
		return userAgentList.get(new Random().nextInt(userAgentList.size()));
	}
}

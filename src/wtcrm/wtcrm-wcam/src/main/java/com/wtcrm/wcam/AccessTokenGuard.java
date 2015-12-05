package com.wtcrm.wcam;

import org.apache.log4j.Logger;

import fomjar.server.FjJsonMsg;
import fomjar.server.FjLoopTask;
import fomjar.server.FjMsg;
import fomjar.server.FjSender;
import fomjar.server.FjToolkit;

public class AccessTokenGuard extends FjLoopTask {

	private static AccessTokenGuard instance = null;
	public static AccessTokenGuard getInstance() {
		if (null == instance) instance = new AccessTokenGuard();
		return instance;
	}
	
	private static final Logger logger = Logger.getLogger(AccessTokenGuard.class);
	
	private static final String TEMPLATE = "https://api.weixin.qq.com/cgi-bin/token?grant_type=%s&appid=%s&secret=%s";
	
	public String token;
	
	public void start() {
		if (isRun()) {
			logger.warn("access-token-guard has already started");
			return;
		}
		Thread wcThread = new Thread(this);
		wcThread.setName("access-token-guard");
		wcThread.start();
	}
	
	public String getToken() {
		return token;
	}
	
	@Override
	public void perform() {
		token = null;
		long defaultInterval = Long.parseLong(FjToolkit.getServerConfig("wcam.reload-token-interval"));
		String url = String.format(TEMPLATE, FjToolkit.getServerConfig("wcam.grant"), FjToolkit.getServerConfig("wcam.appid"), FjToolkit.getServerConfig("wcam.secret"));
		logger.debug("try to get wechat access token");
		FjMsg msg = FjSender.sendHttpRequest("GET", url, null);
		if (!(msg instanceof FjJsonMsg)) {
			logger.error("invalid reponse message when get wechat access token: " + msg);
			setNextRetryInterval(defaultInterval);
			return;
		}
		FjJsonMsg rsp = (FjJsonMsg) msg;
		if (!rsp.json().containsKey("access_token") || !rsp.json().containsKey("expires_in")) {
			logger.error("failed to get wechat access token, error response: " + msg);
			setNextRetryInterval(defaultInterval);
			return;
		}
		token = rsp.json().getString("access_token");
		setNextRetryInterval(Long.parseLong(rsp.json().getString("expires_in")));
		logger.info("got wechat access token successfully: " + rsp);
	}
	
	public void setNextRetryInterval(long seconds) {
		logger.info("will try again after " + seconds + " seconds");
		setInterval(seconds * 1000);
	}
	
}

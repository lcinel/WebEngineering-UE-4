package twitter;

import play.Logger;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TwitterClientImpl implements ITwitterClient {

	private String consumerKey;
	private String consumerSecret;
	private String accessToken;
	private String accessTokenSecret;
	private TwitterFactory twitterFactory;
	private Twitter twitter;
	
	public TwitterClientImpl(){
		
		this.consumerKey = "GZ6tiy1XyB9W0P4xEJudQ";
		this.consumerSecret = "gaJDlW0vf7en46JwHAOkZsTHvtAiZ3QUd2mD1x26J9w";
		this.accessToken = "1366513208-MutXEbBMAVOwrbFmZtj1r4Ih2vcoHGHE2207002";
		this.accessTokenSecret = "RMPWOePlus3xtURWRVnv1TgrjTyK7Zk33evp4KKyA";
		
		twitterFactory = new TwitterFactory();
		twitter = twitterFactory.getInstance();
		
		authenticate();
	}
	
	private void authenticate(){
		
		twitter.setOAuthConsumer(consumerKey, consumerSecret);
		twitter.setOAuthAccessToken(new AccessToken(accessToken, accessTokenSecret));
	}
	
	@Override
	public void publishUuid(TwitterStatusMessage message) throws Exception {
		
		StatusUpdate statusUpdate = new StatusUpdate(message.getTwitterPublicationString());
		Status status = twitter.updateStatus(statusUpdate);
		
		String statusSource = status.getSource();
		String statusText = status.getText();
		String statusUserName = status.getUser().getName();
		
		Logger.info("Twitter Response caught:"
				+ "\n\tSource: " + statusSource
				+ "\n\tText: " + statusText
				+ "\n\tUser: " + statusUserName);
	}

}

package controllers;

import models.QuizUser;
import play.mvc.Http.Context;
import play.mvc.Http.Session;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator {

	private static final String USER_ID = "userId";

	public static void addAuthentication(Session session, QuizUser user) {
		session.clear();
		session.put(USER_ID, String.valueOf(user.getId()));
	}
	
	public static String getAuthentication(Session session) {
		return session.get(USER_ID);
	}

	@Override
	public String getUsername(Context ctx) {
		return ctx.session().get(USER_ID);
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return redirect(routes.Authentication.login());
	}
}
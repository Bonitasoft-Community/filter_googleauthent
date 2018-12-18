package com.bonitasoft.googleauthent;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bonitasoft.console.common.server.login.HttpServletRequestAccessor;
import org.bonitasoft.console.common.server.utils.PermissionsBuilder;
import org.bonitasoft.console.common.server.utils.PermissionsBuilderAccessor;
import org.bonitasoft.console.common.server.utils.SessionUtil;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.Group;
import org.bonitasoft.engine.identity.GroupNotFoundException;
import org.bonitasoft.engine.identity.Role;
import org.bonitasoft.engine.identity.RoleNotFoundException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileMember;
import org.bonitasoft.engine.profile.ProfileMemberSearchDescriptor;
import org.bonitasoft.engine.profile.ProfileSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

// see https://developers.google.com/identity/sign-in/web/backend-auth#send-the-id-token-to-your-server
public class FilterGoogle implements Filter {

    private final String versionFilter = "V1.1";

    private String googleServerClientId = "";
    public Logger logger = Logger.getLogger(FilterGoogle.class.getName());

    FilterConfig filterConfig = null;
    /**
     * the list of technical user is a set of string like "user|install, user|walter.bates, profile|admin"
     * Accepted header are user, group, role, profile
     */
    private List<String> listTechnicalUsers = new ArrayList<String>();
    /**
     * the resolveTechicalUser is a list of physical user. The listTechnical contains a header, to match different population.
     */
    private List<String> listResolveTechnicalUsers = null;

    /**
     * to resolve the list, a technicalLogin is needed.
     */
    private String technicalLoginPassword = null;

    boolean isAcceptableClassicalLogin = false;
    String bonitaPassword = "";
    boolean isLog = false;
    boolean isAllowPing = false;

    private Integer CounterReqId = new Integer(0);;

    public void init(final FilterConfig filterConfig) throws ServletException {
        logger.info("FilterGoogle:init -----------------------------------filterGoogle: " + versionFilter + " init --------------");
        //-------------
        googleServerClientId = filterConfig.getInitParameter("googleServerClientId");
        if (googleServerClientId == null) {
            logger.severe("FilterGoogle:init No Google Authent is given, service can be run !");
        }
        logger.info("FilterGoogle:init  googleServerClientId[" + googleServerClientId + "]");

        //-------------
        String technicalUsersSt = filterConfig.getInitParameter("technicalsUsers");
        if (technicalUsersSt == null) {
            technicalUsersSt = "user|install";
        }
        final StringTokenizer st = new StringTokenizer(technicalUsersSt, ",");
        while (st.hasMoreTokens()) {
            listTechnicalUsers.add(st.nextToken().toLowerCase().trim());
        }
        logger.info("FilterGoogle:init  technicalsUsers[" + technicalUsersSt + "]");

        // ------------- technical login
        technicalLoginPassword = filterConfig.getInitParameter("technicalLoginPassword");
        logger.info("FilterGoogle:init  technicalLoginPassword[" + technicalLoginPassword + "]");

        //-------------
        final String isAcceptableClassicalLoginSt = filterConfig.getInitParameter("acceptClassicalLogin");
        isAcceptableClassicalLogin = "TRUE".equalsIgnoreCase(isAcceptableClassicalLoginSt);
        logger.info("FilterGoogle:init  acceptClassicalLogin[" + isAcceptableClassicalLogin + "] st[" + isAcceptableClassicalLoginSt + "]");

        //-------------
        bonitaPassword = filterConfig.getInitParameter("bonitaPassword");
        logger.info("FilterGoogle:init  bonitaPassword[" + bonitaPassword + "]");

        //-------------
        final String logSt = filterConfig.getInitParameter("log");
        isLog = "TRUE".equalsIgnoreCase(logSt);
        logger.info("FilterGoogle:init  log[" + isLog + "] (logSt[" + logSt + "]");

        //-------------
        final String pingSt = filterConfig.getInitParameter("ping");
        isAllowPing = "TRUE".equalsIgnoreCase(pingSt);
        logger.info("FilterGoogle:init  ping[" + isAllowPing + "] (pingSt[" + pingSt + "]");

        this.filterConfig = filterConfig;
    }

    /**
     * Each URL come
     */
    public void doFilter(final ServletRequest request,
            final ServletResponse servletResponse, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        final HttpServletRequest httpRequest = (HttpServletRequest) request;


        if (isLog)
        {
            synchronized (CounterReqId) {
                CounterReqId = Integer.valueOf(CounterReqId.intValue() + 1);

            }
        }
        final int reqId = CounterReqId;
        String traceRequest = "filterGoogle " + versionFilter;
        try
        {

            final String url = httpRequest.getRequestURL().toString();

            traceRequest += "#" + reqId + " [" + url + "]";

            // ------------------------------ Ping
            if (url.endsWith("/filtergoogleping"))
            {
                if (isAllowPing)
                {
                    logger.info("FilterGoogle:filter #" + reqId + " ----------------------------------- ping request");
                    PrintWriter out;
                    try {
                        out = servletResponse.getWriter();

                        out.println("<html><body>Filter activity");
                        out.println("Filter Google V1.0.4 is active<br>");
                        out.println("</body></html>");

                        out.flush();
                    } catch (final IOException e) {
                        logger.severe("filterGoogle : exception during write " + e.toString());
                    }
                    return;
                } else {
                    logger.info("FilterGoogle:filter #" + reqId + "----------------------------------- ping request, but not allow it");
                }
            }

            // ---------------- check the connection
            final HttpServletRequestAccessor requestAccessor = new HttpServletRequestAccessor(httpRequest);
            if (requestAccessor != null) {
                final APISession apiSession = requestAccessor.getApiSession();
                if (apiSession != null) {
                    // logger.info("filterGoogle: Already connected ");
                    chain.doFilter(httpRequest, servletResponse);
                    return;
                }

            }

            if (isLog) {
                logger.info("-----------------------------------filterGoogle #" + reqId + " " + versionFilter + " Url[" + url
                        + "]--------------");
            }
            final String idTokenGoogle = request.getParameter("idtokengoogle");
            final String nameGoogle = request.getParameter("namegoogle");

            boolean pleaseRejectInFinalTheRequest = false;
            // get the information
            final String userNameLoginService = request.getParameter("username");
            final String idTokenTrace = idTokenGoogle == null ? "null" : idTokenGoogle.length() > 10 ? idTokenGoogle.substring(0, 10) : idTokenGoogle;
            traceRequest += ",username[" + userNameLoginService + "] idTokenGoogle(10)=[" + idTokenTrace + "]";
            if (isLog) {
                logger.info("-----------------------------------filterGoogle: #" + reqId + " LoginService username[" + userNameLoginService
                        + "] idTokenGoogle(10)=[" + idTokenTrace + "]");
            }

            // ----------- two step login : first, we look if this is a direct login.
            // If yes, and if we accept the direct login, then let's pass

            // loginservice : user click on "login" button
            // loging.jsp : this is a redirect URL

            // we accept a login service but if token !=null && username==null => this is a Token login on Redirect for example
            if (url.indexOf("/loginservice") != -1
                    && idTokenGoogle == null && userNameLoginService != null)
            {
                if ("install".equals(userNameLoginService)) {
                    if (isLog) {
                        logger.info("FilterGoogle:filter #" + reqId + " -----------------------------------filterGoogle: #" + reqId
                                + " Install login *ENDFILTER* traceRequest:" + traceRequest);
                    }
                    chain.doFilter(httpRequest, servletResponse);
                    return;
                }
                //
                if (isAcceptableClassicalLogin)
                {
                    if (isLog) {
                        logger.info("FilterGoogle:filter #" + reqId + " -----------------------------------filterGoogle: #" + reqId
                                + " Accept Classical Login *ENDFILTER* traceRequest:" + traceRequest);
                    }
                    // ok, we accept this connection
                    chain.doFilter(httpRequest, servletResponse);
                    return;
                }
                final boolean isATechnicalUser = isATechnicalUser(userNameLoginService, reqId);
                if (isATechnicalUser)
                {
                    if (isLog) {
                        logger.info("FilterGoogle:filter #" + reqId
                                + " -----------------------------------filterGoogle: Technical User *ENDFILTER* traceRequest:" + traceRequest);
                    }
                    // ok, do nothing, it's a technical user
                    chain.doFilter(httpRequest, servletResponse);
                    return;
                }

                // ok, not a technical user, not accept classical login
                traceRequest += ",Not ClassicalLogin, not a TechnicalUser [" + listResolveTechnicalUsers + "]:prepare to reject";
                pleaseRejectInFinalTheRequest = true; // but let's a second chance by Google
            }
            else {
                traceRequest += ",Not a explicit login URL";
                // google example here https://raw.githubusercontent.com/lmoroney/serverauth/master/LmauthtestServlet.java
            }

            // http://localhost:8080/bonita/API/form/mapping?c=10&p=0&f=processDefinitionId%3D9090055435485955288&f=type%3DPROCESS_START&useranonymous=true
            if (isLog) {
                logger.info("FilterGoogle:filter #" + reqId + " nameGoogle[" + nameGoogle + "] idtokengoogle=[" + idTokenGoogle + "]");
            }

            if (idTokenGoogle != null)
            {
                traceRequest += ",IdTokenGoogle detected";
                String userNameFromGoogle = null;
                try {

                    final NetHttpTransport transport = new NetHttpTransport();
                    final GsonFactory jsonFactory = new GsonFactory();

                    final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                            .setAudience(Arrays.asList(googleServerClientId))
                            // To learn about getting a Server Client ID, see this link
                            // https://developers.google.com/identity/sign-in/android/start
                            // And follow step 4
                            // .setIssuer("https://accounts.google.com").build();
                            // .setIssuer("http://localhost:8080/bonita")
                            .build();

                    if (isLog) {
                        logger.info("FilterGoogle:filter #" + reqId + " Call verifier now");
                    }

                    final GoogleIdToken idToken = verifier.verify(idTokenGoogle);
                    if (isLog) {
                        logger.info("FilterGoogle:filter #" + reqId + " get a idToken ? " + (idToken == null ? "No" : "Yes !!"));
                    }
                    if (idToken != null) {
                        final Payload payload = idToken.getPayload();

                        userNameFromGoogle = (String) payload.get("name");
                        traceRequest += ",Google accept userNameFromGoogle[" + userNameFromGoogle + "]  UserId[" + payload.getSubject() + "]";
                        if (isLog) {
                            logger.info("FilterGoogle:filter #" + reqId + " TOKEN : User[" + userNameFromGoogle + "] UserId[" + payload.getSubject() + "]");

                        }
                    }

                    /*
                     * if (userName == null) {
                     * logger.info("filterGoogle: Use the GoogleName");
                     * userName = nameGoogle;
                     * }
                     */

                    // ------------ connection to Bonita
                    if (userNameFromGoogle != null)
                    {
                        userNameFromGoogle = userNameFromGoogle.replace(" ", "_");
                        // a fake password
                        final String userPassword = bonitaPassword;
                        // so log the the user now
                        if (isLog) {
                            logger.info("FilterGoogle:filter #" + reqId + " Bonita Connection with UserName[" + userNameFromGoogle + "] url=["
                                    + httpRequest.getProtocol() + ":"
                                    + httpRequest.getLocalName() + ":"
                                    + httpRequest.getLocalPort() + "]");
                        }

                        // let connect on this server
                        final Map<String, String> map = new HashMap<String, String>();
                        APITypeManager.setAPITypeAndParams(ApiAccessType.LOCAL, map);

                        final LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();

                        // log in to the tenant to create a session
                        final APISession apiSession = loginAPI.login(userNameFromGoogle, userPassword);
                        // set the session in the TomcatSession
                        logger.info("FilterGoogle:filter #" + reqId
                                + " ----------------------------------- filterGoogle:  *ENDFILTER* Connection success with["
                                + userNameFromGoogle + "] "
                                + traceRequest);

                        final HttpSession httpSession = httpRequest.getSession();
                        final org.bonitasoft.web.rest.model.user.User user = new org.bonitasoft.web.rest.model.user.User(userNameFromGoogle,
                                Locale.ENGLISH.getDisplayName());
                        final PermissionsBuilder permissionsBuilder = PermissionsBuilderAccessor.createPermissionBuilder(apiSession);
                        final Set<String> permissions = permissionsBuilder.getPermissions();
                        SessionUtil.sessionLogin(user, apiSession, permissions, httpSession);
                        chain.doFilter(httpRequest, servletResponse);
                        return;
                    }
                    else {
                        logger.severe("FilterGoogle:filter Username is null *ENDFILTER* traceRequest:" + traceRequest);
                    }

                    chain.doFilter(httpRequest, servletResponse);

                    // redirect to the login page
                    return;

                } catch (final Exception e) {
                    final StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    final String exceptionDetails = sw.toString();

                    logger.severe("FilterGoogle:filter #" + reqId + " GoogleGeneralSecurity error " + exceptionDetails + " traceRequest=" + traceRequest);
                }

            } // idTOken

            if (pleaseRejectInFinalTheRequest)
            {
                // no, we stop !
                logger.severe("---------------------------------- FilterGoogle:filter#" + reqId + " Rejected [401] *ENDFILTER* [" + traceRequest + "]");

                httpResponse.setStatus(401); // Not authorized
                return;
            }

            // not acceptable, but let's Bonita do it's job
            logger.info("---------------------------------- FilterGoogle:filter #" + reqId + "  *ENDFILTER* [" + traceRequest + "]");
            chain.doFilter(httpRequest, servletResponse);
            return;
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();

            logger.severe("---------------------------------- FilterGoogle:filter #" + reqId + " *ENDFILTER* GoogleException error " + exceptionDetails
                    + " traceRequest=" + traceRequest);
            chain.doFilter(httpRequest, servletResponse);
            return;
        }
    }

    public void destroy() {
        // TODO Auto-generated method stub

    }

    /**
     * calculate if the username is part of the technical list or not
     *
     * @param userName
     * @return
     */
    private boolean isATechnicalUser(final String userName, final int reqId) {

        if (userName == null) {
            return false;
        } else
        {
            if (listResolveTechnicalUsers == null)
            {
                resolveTechnicalUsers(null, reqId);
            }
            if (listResolveTechnicalUsers != null) {
                for (final String user : listResolveTechnicalUsers) {

                    if (userName.equalsIgnoreCase(user)) {
                        return true;

                    }
                }
            }
            logger.info("FilterGoogle:isATechnicalUser #" + reqId + " user[" + userName + "] not in [" + listResolveTechnicalUsers + "]");
        }
        return false;

    }

    /**
     * calculate the technicalUsers
     */
    public void resolveTechnicalUsers(APISession apiSession, final int reqId)
    {

        logger.info("FilterGoogle:resolveTechnicalUsers #" + reqId + " ~~~~~~~~~~~~~~~~~~~~~");
        IdentityAPI identityAPI = null;
        ProfileAPI profileAPI = null;

        listResolveTechnicalUsers = new ArrayList<String>();
        if (listTechnicalUsers == null) {
            return;
        }
        // ok, parse it
        for (final String oneItem : listTechnicalUsers)
        {
            // logger.info("FilterGoogle:filter oneItem=["+oneItem+"]");

            if (oneItem.startsWith("user|"))
            {
                logger.info("FilterGoogle:filter #" + reqId + " User[" + oneItem.substring("user|".length()) + "]");
                listResolveTechnicalUsers.add(oneItem.substring("user|".length()));
                continue;
            }

            // A this moment, we need a connection
            if (apiSession == null)
            {
                apiSession = login(reqId);
            }
            if (identityAPI == null && apiSession != null)
            {
                // logger.info("FilterGoogle:filter getIdentity");
                try {
                    identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
                } catch (final Exception e) {
                    logger.severe("FilterGoogle:filter #" + reqId + " Can't access the IdentityAPI :" + e.toString());
                }
            }
            if (profileAPI == null && apiSession != null)
            {
                // logger.info("FilterGoogle:filter getProfile");
                try {
                    profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);
                } catch (final Exception e) {
                    logger.severe("FilterGoogle:filter #" + reqId + " Can't access the profileAPI :" + e.toString());
                }

            }
            if (identityAPI == null) {
                logger.info("FilterGoogle:filter #" + reqId + " no IdentityAPI, can't continue");
                continue;
            }
            if (profileAPI == null) {
                logger.info("FilterGoogle:filter #" + reqId + " no profileAPI, can't continue");
                continue;
            }

            // continue the treatment
            SearchOptionsBuilder searchOptionBuilder = null;
            String parameterSearch = "";
            if (oneItem.startsWith("group|"))
            {
                final String groupPath = oneItem.substring("group|".length());
                logger.info("FilterGoogle:filter #" + reqId + " Group[" + groupPath + "]");
                Group group = null;
                try {
                    group = identityAPI.getGroupByPath(groupPath);
                } catch (final GroupNotFoundException e) {
                    logger.severe("FilterGoogle:filter #" + reqId + " Grouppath[" + groupPath + "] does not exist");
                }
                if (group != null)
                {
                    parameterSearch += ",Group[" + groupPath + "] ID[" + group.getId() + "]";
                    searchOptionBuilder = new SearchOptionsBuilder(0, 1000);
                    searchOptionBuilder.filter(UserSearchDescriptor.GROUP_ID, group.getId());
                }
                else
                {
                    logger.severe("FilterGoogle:filter #" + reqId + " No Group[" + groupPath + "] found");
                }
            }
            else if (oneItem.startsWith("role|"))
            {
                final String roleName = oneItem.substring("role|".length());
                logger.info("FilterGoogle:filter #" + reqId + " Role[" + roleName + "]");
                Role role = null;
                try {
                    role = identityAPI.getRoleByName(roleName);
                } catch (final RoleNotFoundException e) {
                    logger.severe("FilterGoogle:filter #" + reqId + " Rolename[" + roleName + "] does not exist");
                }
                if (role != null)
                {
                    parameterSearch += "Role[" + roleName + "] ID[" + role.getId() + "]";
                    searchOptionBuilder = new SearchOptionsBuilder(0, 1000);
                    searchOptionBuilder.filter(UserSearchDescriptor.ROLE_ID, role.getId());
                }
                else
                {
                    logger.severe("FilterGoogle:filter #" + reqId + " No Role [" + roleName + "] found");
                }
            }
            else if (oneItem.startsWith("profile|"))
            {
                final String profileName = oneItem.substring("profile|".length());
                logger.info("FilterGoogle:filter #" + reqId + " profile[" + profileName + "]");

                final SearchResult<ProfileMember> resultMember = searchProfileMember(profileName, profileAPI, reqId);
                if (resultMember != null)
                {
                    for (final ProfileMember profileMember : resultMember.getResult())
                    {
                        if (profileMember.getUserId() > 0) {
                            logger.info("FilterGoogle:filter #" + reqId + " profileMember USERID[" + profileMember.getUserId() + "]");

                            try
                            {
                                final User user = identityAPI.getUser(profileMember.getUserId());
                                listResolveTechnicalUsers.add(user.getUserName());
                            } catch (final Exception e)
                            {
                                logger.severe("FilterGoogle:filter #" + reqId + " userId[" + profileMember.getUserId() + "] register in profile, but no exist");
                            }
                        }
                        if (profileMember.getRoleId() > 0) {
                            logger.info("FilterGoogle:filter profileMember ROLEID[" + profileMember.getRoleId() + "]");
                            if (searchOptionBuilder == null) {
                                searchOptionBuilder = new SearchOptionsBuilder(0, 1000);
                            } else {
                                searchOptionBuilder.or();
                            }

                            parameterSearch += ", RoleID(profile)[" + profileMember.getRoleId() + "]";
                            searchOptionBuilder.filter(UserSearchDescriptor.ROLE_ID, profileMember.getRoleId());
                        }
                        if (profileMember.getGroupId() > 0) {
                            logger.info("FilterGoogle:filter #" + reqId + " profileMember GROUPID[" + profileMember.getGroupId() + "]");

                            if (searchOptionBuilder == null) {
                                searchOptionBuilder = new SearchOptionsBuilder(0, 1000);
                            } else {
                                searchOptionBuilder.or();
                            }

                            parameterSearch += ", GroupID(profile)[" + profileMember.getGroupId() + "]";
                            searchOptionBuilder.filter(UserSearchDescriptor.GROUP_ID, profileMember.getGroupId());
                        }
                    }
                }
            }
            else
            {
                logger.severe("FilterGoogle:filter #" + reqId + " listTechnicalUsers item[" + oneItem
                        + "] doest not respect the expecting format : user|<user> group|<group> role|<role> profile|<profile");
                continue;
            }

            // ---- search asked ?
            if (searchOptionBuilder != null)
            {
                // logger.info("FilterGoogle:filter we got a SearchOption : search["+parameterSearch+"]");

                SearchResult<User> searchUser;
                try {
                    searchUser = identityAPI.searchUsers(searchOptionBuilder.done());
                    String collectUser = "";
                    for (final User user : searchUser.getResult()) {
                        collectUser += user.getUserName() + ",";
                        listResolveTechnicalUsers.add(user.getUserName());
                    }
                    logger.severe("FilterGoogle:filter #" + reqId + " search[" + parameterSearch + "] Add users[" + collectUser + "]");
                } catch (final SearchException e) {
                    logger.severe("FilterGoogle:filter #" + reqId + " Error on search[" + parameterSearch + "] " + e.toString());

                }
            }

        }
        logger.info("FilterGoogle:filter #" + reqId + " List Technical user =>" + listResolveTechnicalUsers);
    }

    /**
     * login as the technicalpassword
     *
     * @return
     */
    public APISession login(final int reqId)
    {
        final Map<String, String> map = new HashMap<String, String>();
        APITypeManager.setAPITypeAndParams(ApiAccessType.LOCAL, map);

        // Set the username and password
        // final String username = "helen.kelly";
        final int posSlash = technicalLoginPassword.indexOf("/");

        final String username = posSlash > -1 ? technicalLoginPassword.substring(0, posSlash) : technicalLoginPassword;
        final String password = posSlash > -1 ? technicalLoginPassword.substring(posSlash + 1) : "";
        logger.info("FilterGoogle:filter #" + reqId + " Login with userName[" + username + "]");

        // get the LoginAPI using the TenantAPIAccessor
        LoginAPI loginAPI;
        try {
            loginAPI = TenantAPIAccessor.getLoginAPI();
            // log in to the tenant to create a session
            final APISession session = loginAPI.login(username, password);
            return session;
        } catch (final Exception e)
        {
            logger.severe("FilterGoogle:filter #" + reqId + " during login " + e.toString());
        }
        return null;
    }

    /**
     * @param profileName
     * @param profileAPI
     * @return
     */
    private SearchResult<ProfileMember> searchProfileMember(final String profileName, final ProfileAPI profileAPI, final int reqId)
    {
        try
        {
            final SearchOptionsBuilder searchOptionBuilderProfile = new SearchOptionsBuilder(0, 1000);
            searchOptionBuilderProfile.filter(ProfileSearchDescriptor.NAME, profileName);
            final SearchResult<Profile> searchProfile = profileAPI.searchProfiles(searchOptionBuilderProfile.done());
            if (searchProfile.getResult().size() == 0)
            {
                logger.severe("FilterGoogle:searchProfileMember #" + reqId + " [" + profileName + "] does not exist ");
                return null;
            }

            final SearchOptionsBuilder searchOptionBuilderProfileMember = new SearchOptionsBuilder(0, 1000);
            searchOptionBuilderProfileMember.filter(ProfileMemberSearchDescriptor.PROFILE_ID, searchProfile.getResult().get(0).getId());

            // now searh pr
            final SearchResult<ProfileMember> resultMember = profileAPI.searchProfileMembers("user", searchOptionBuilderProfileMember.done());
            return resultMember;
        } catch (final Exception e)
        {
            logger.severe("FilterGoogle:searchProfileMember  #" + reqId + " [" + profileName + "] " + e.toString());
            return null;
        }

    }

    public void setlistTechnicalUsers(final List<String> listTechnicalUsers)
    {
        this.listTechnicalUsers = listTechnicalUsers;
    }

    public void addInListTechnicalUsers(final String oneItem)
    {
        listTechnicalUsers.add(oneItem);
    }

}

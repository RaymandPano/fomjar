package com.ski.game.session;

import org.apache.log4j.Logger;

import com.ski.common.SkiCommon;

import fomjar.server.FjMessageWrapper;
import fomjar.server.FjServerToolkit;
import fomjar.server.FjServerToolkit.FjAddress;
import fomjar.server.msg.FjDscpMessage;
import fomjar.server.session.FjSessionContext;
import fomjar.server.session.FjSessionPath;
import fomjar.server.session.FjSessionTask;
import net.sf.json.JSONObject;

/**
 * 请求来自CDB，转发产品详单至WCA
 * 
 * @author fomjar
 */
public class SessionTaskQueryOrderFromCDB implements FjSessionTask {
    
    private static final Logger logger = Logger.getLogger(SessionTaskQueryOrderFromCDB.class);

    @Override
    public boolean onSession(FjSessionPath path, FjMessageWrapper wrapper) {
        FjSessionContext context = path.context();
        String server = context.server();
        FjDscpMessage msg = (FjDscpMessage) wrapper.message();
        if (!msg.fs().startsWith("cdb")) {
            logger.error("invalid message, not come from cdb: " + msg);
            return false;
        }
        
        JSONObject args = new JSONObject();
        args.put("user",    context.getString("caid"));
        args.put("content", createUserResponseContent4Apply(context, msg));
        
        FjDscpMessage msg_wca = new FjDscpMessage();
        msg_wca.json().put("fs",   server);
        msg_wca.json().put("ts",   "wca");
        msg_wca.json().put("sid",  context.sid());
        msg_wca.json().put("inst", SkiCommon.ISIS.INST_USER_RESPONSE);
        msg_wca.json().put("args", args);
        FjServerToolkit.getSender(server).send(msg_wca);
        
        return true;
    }

    private static String createUserResponseContent4Apply(FjSessionContext scb, FjDscpMessage msg) {
        StringBuffer content = new StringBuffer("【游戏清单】\n\n");
        int    code = msg.argsToJsonObject().getInt("code");
        if (SkiCommon.CODE.CODE_SYS_SUCCESS != code) return "database operate failed";
        
        String[] products = msg.argsToJsonObject().getJSONArray("desc").getJSONArray(0).getString(2).split("\n");
        for (String productString : products) {
            String[] product = productString.split("\t");
            /**
             * product[0] = product type
             * product[1] = instance id
             * product[2] = product name
             * product[3] = game account
             */
            FjAddress address = FjServerToolkit.getSlb().getAddress("wcweb");
            String url = String.format("http://%s:%d/wcweb?user=%s&inst=%s&content=%s", 
                    address.host,
                    address.port,
                    scb.getString("caid"),
                    Integer.toHexString(SkiCommon.ISIS.INST_ECOM_APPLY_RETURN),
                    product[1]);
            content.append(String.format("<a href='%s'>《%s》</a>\n账号(%s)：%s\n\n",
                    url,
                    product[2],
                    0 == Integer.parseInt(product[0], 16) ? "A类" : "B类",
                    product[3]));
        }
        return content.toString();
    }

}

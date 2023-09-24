package com.sino.auth.server.vo;

import lombok.Data;

/**
 * Auto-generated: 2023-09-24 1:26:6
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class SocialUser{

    private String access_token;
    private String uid;
    private String token_type;
    private long expires_in;
    private String refresh_token;
    private String scope;
    private long created_at;

    private String name;
    private String email;


}
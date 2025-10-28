package io.mosip.datashare.dto;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component("authorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.durian")
@Getter
@Setter
public class AuthorizedRolesDTO {


    private List<String> postcreatepolicyIdsubscriberid;
    
}

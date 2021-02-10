package eu.domibus.core.ebms3.mapper.signal;

import eu.domibus.api.ebms3.model.Ebms3Receipt;
import eu.domibus.api.model.ReceiptEntity;
import eu.domibus.core.ebms3.mapper.Ebms3CentralMapperConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Mapper(config = Ebms3CentralMapperConfig.class)
public interface Ebms3ReceiptMapper {

    @Mapping(source = "any", target = "any")
    @BeanMapping(ignoreUnmappedSourceProperties = {"entityId", "creationTime", "modificationTime", "createdBy", "modifiedBy"})
    Ebms3Receipt receiptEntityToEbms3(ReceiptEntity receipt);

    @Mapping(target = "entityId", ignore = true)
    @Mapping(target = "creationTime", ignore = true)
    @Mapping(target = "modificationTime", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @InheritInverseConfiguration
    ReceiptEntity receiptEbms3ToEntity(Ebms3Receipt ebms3Receipt);
}
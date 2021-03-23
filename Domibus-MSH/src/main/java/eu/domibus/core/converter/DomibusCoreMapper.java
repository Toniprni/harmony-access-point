package eu.domibus.core.converter;

import eu.domibus.api.audit.AuditLog;
import eu.domibus.api.cluster.Command;
import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.model.*;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.party.Party;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.pmode.PModeArchiveInfo;
import eu.domibus.api.property.DomibusProperty;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.encryption.PasswordEncryptionResult;
import eu.domibus.api.routing.BackendFilter;
import eu.domibus.api.routing.RoutingCriteria;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.api.user.User;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.core.alerts.model.mapper.EventMapper;
import eu.domibus.core.alerts.model.persist.Alert;
import eu.domibus.core.audit.model.Audit;
import eu.domibus.core.audit.model.mapper.AuditMapper;
import eu.domibus.core.clustering.CommandEntity;
import eu.domibus.core.crypto.spi.CertificateEntrySpi;
import eu.domibus.core.crypto.spi.DomainSpi;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.logging.LoggingEntry;
import eu.domibus.core.message.MessageLogInfo;
import eu.domibus.core.message.attempt.MessageAttemptEntity;
import eu.domibus.core.party.PartyResponseRo;
import eu.domibus.core.party.ProcessRo;
import eu.domibus.core.plugin.routing.BackendFilterEntity;
import eu.domibus.core.plugin.routing.RoutingCriteriaEntity;
import eu.domibus.core.property.DomibusPropertiesFilter;
import eu.domibus.core.replication.UIMessageDiffEntity;
import eu.domibus.core.replication.UIMessageEntity;
import eu.domibus.core.user.plugin.AuthenticationEntity;
import eu.domibus.ext.domain.*;
import eu.domibus.web.rest.ro.*;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author Ioana Dragusanu (idragusa)
 * @since 4.1
 */
//@Mapper(uses = {EventMapper.class, AuditMapper.class}, componentModel = "spring")
public interface DomibusCoreMapper {

    @Mapping(source = "id", target = "entityId")
    @Mapping(target = "initiatorPartiesXml", ignore = true)
    @Mapping(target = "responderPartiesXml", ignore = true)
    @Mapping(target = "initiatorParties", ignore = true)
    @Mapping(target = "responderParties", ignore = true)
    @Mapping(target = "legs", ignore = true)
    Process processAPIToProcess(eu.domibus.api.process.Process process);

    @InheritInverseConfiguration
    eu.domibus.api.process.Process processToProcessAPI(Process process);

    DomainSpi domainToDomainSpi(Domain domain);

    Domain domainSpiToDomain(DomainSpi domainSpi);

    DomainDTO domainToDomainDTO(Domain domain);

    Domain domainDTOToDomain(DomainDTO domain);

    @Mapping(target = "persisted", ignore = true)
    MessageFilterRO backendFilterToMessageFilterRO(BackendFilter backendFilter);

    @Mapping(target = "active", ignore = true)
    BackendFilter messageFilterROToBackendFilter(MessageFilterRO backendFilterEntity);

    BackendFilterEntity backendFilterToBackendFilterEntity(BackendFilter backendFilter);

    @Mapping(target = "active", ignore = true)
    BackendFilter backendFilterEntityToBackendFilter(BackendFilterEntity backendFilterEntity);

    @InheritInverseConfiguration
    RoutingCriteria routingCriteriaEntityToRoutingCriteria(RoutingCriteriaEntity routingCriteriaEntity);

    @Mapping(target = "inputPattern", ignore = true)
    @Mapping(target = "tooltip", ignore = true)
    RoutingCriteriaEntity routingCriteriaToRoutingCriteriaEntity(RoutingCriteria routingCriteria);

    @Mapping(source = "id", target = "entityId")
    MessageAttemptEntity messageAttemptToMessageAttemptEntity(MessageAttempt messageAttempt);

    @InheritInverseConfiguration
    MessageAttempt messageAttemptEntityToMessageAttempt(MessageAttemptEntity messageAttemptEntity);

    @InheritInverseConfiguration
    PropertyDTO propertyToPropertyDTO(Property property);

    @Mapping(target = "entityId", ignore = true)
    Property propertyDTOToProperty(PropertyDTO propertyDTO);

    @InheritInverseConfiguration
    PartyIdDTO partyIdToPartyIdDTO(PartyId partyId);

    @Mapping(target = "entityId", ignore = true)
    PartyId partyIdDTOToPartyId(PartyIdDTO partyIdDTO);

    @InheritInverseConfiguration
    PartInfoDTO partInfoToPartInfoDTO(PartInfo partInfo);

    @Mapping(target = "entityId", ignore = true)
    @Mapping(target = "payloadDatahandler", ignore = true)
    @Mapping(target = "binaryData", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "length", ignore = true)
    PartInfo partInfoDTOToPartInfo(PartInfoDTO partInfoDTO);

    @InheritInverseConfiguration
    UserMessageDTO userMessageToUserMessageDTO(UserMessage userMessage);

    @Mapping(target = "entityId", ignore = true)
    @Mapping(target = "messageFragment", ignore = true)
    @Mapping(target = "splitAndJoin", ignore = true)
    UserMessage userMessageDTOToUserMessage(UserMessageDTO userMessageDTO);

    @InheritInverseConfiguration
    PullRequestDTO pullRequestToPullRequestDTO(PullRequest pullRequest);

    PullRequest pullRequestDTOToPullRequest(PullRequestDTO pullRequestDTO);

    @Mapping(target = "authenticationType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "suspended", ignore = true)
    PluginUserRO authenticationEntityToPluginUserRO(AuthenticationEntity authenticationEntity);

    @Mapping(target = "attemptCount", ignore = true)
    @Mapping(target = "suspensionDate", ignore = true)
    @Mapping(target = "passwordChangeDate", ignore = true)
    @Mapping(target = "defaultPassword", ignore = true)
    @Mapping(target = "backend", ignore = true)
    AuthenticationEntity pluginUserROToAuthenticationEntity(PluginUserRO pluginUserRO);

    @Mapping(target = "current", ignore = true)
    PModeResponseRO pModeArchiveInfoToPModeResponseRO(PModeArchiveInfo pModeArchiveInfo);

    @InheritInverseConfiguration
    PModeArchiveInfo pModeResponseROToPModeArchiveInfo(PModeResponseRO pModeResponseRO);

    TrustStoreRO trustStoreEntryToTrustStoreRO(TrustStoreEntry trustStoreEntry);

    TrustStoreEntry trustStoreROToTrustStoreEntry(TrustStoreRO trustStoreRO);

    AuditResponseRo auditLogToAuditResponseRo(AuditLog auditLog);

    AuditLog auditResponseRoToAuditLog(AuditResponseRo auditResponseRo);

    DomainRO domainToDomainRO(Domain domain);

    Domain domainROToDomain(DomainRO domain);

    UserResponseRO userToUserResponseRO(User user);

    User userResponseROToUser(UserResponseRO user);

    eu.domibus.core.user.ui.User userApiToUserSecurity(User user);

    User userSecurityToUserApi(eu.domibus.core.user.ui.User user);

    AuditLog auditToAuditLog(Audit audit);

    LoggingLevelRO loggingEntryToLoggingLevelRO(LoggingEntry loggingEntry);

    LoggingEntry loggingLevelROToLoggingEntry(LoggingLevelRO loggingLevelRO);

    Party configurationPartyToParty(eu.domibus.common.model.configuration.Party party);

    eu.domibus.common.model.configuration.Party partyToConfigurationParty(Party party);

    @Mapping(source = "entityId", target = "id")
    eu.domibus.api.process.Process processRoToProcess(ProcessRo processRo);

    @InheritInverseConfiguration
    ProcessRo processToProcessRo(eu.domibus.api.process.Process process);

    Alert alertServiceToAlertPersist(eu.domibus.core.alerts.model.service.Alert alert);

    eu.domibus.core.alerts.model.service.Alert alertPersistToAlertService(Alert alert);

    MessageLogInfo messageLogROToMessageLogInfo(MessageLogRO messageLogRO);

    MessageLogRO messageLogInfoToMessageLogRO(MessageLogInfo messageLogInfo);

    UIMessageDiffEntity uiMessageEntityToUIMessageDiffEntity(UIMessageEntity uiMessageEntity);

    UIMessageEntity uiMessageDiffEntityToUIMessageEntity(UIMessageDiffEntity uiMessageEntity);

    @Mapping(source = "fromId", target = "fromPartyId")
    @Mapping(source = "toId", target = "toPartyId")
    @Mapping(source = "fromScheme", target = "originalSender")
    @Mapping(source = "toScheme", target = "finalRecipient")
    MessageLogRO uiMessageEntityToMessageLogRO(UIMessageEntity uiMessageEntity);

    @InheritInverseConfiguration
    UIMessageEntity messageLogROToUIMessageEntity(MessageLogRO messageLogRO);

    @Mapping(source = "fromId", target = "fromPartyId")
    @Mapping(source = "toId", target = "toPartyId")
    @Mapping(source = "fromScheme", target = "originalSender")
    @Mapping(source = "toScheme", target = "finalRecipient")
    MessageLogInfo uiMessageEntityToMessageLogInfo(UIMessageEntity uiMessageEntity);

    @InheritInverseConfiguration
    UIMessageEntity messageLogInfoToUIMessageEntity(MessageLogInfo messageLogInfo);

    SignalMessageLog uiMessageEntityToSignalMessageLog(UIMessageEntity uiMessageEntity);

    UIMessageEntity signalMessageLogToUIMessageEntity(SignalMessageLog signalMessageLog);

    @InheritInverseConfiguration
    UserMessageLog uiMessageEntityToUserMessageLog(UIMessageEntity uiMessageEntity);

    @Mapping(source = "messageInfo.refToMessageId", target = "refToMessageId")
    UIMessageEntity userMessageLogToUIMessageEntity(UserMessageLog userMessageLog);

    CommandEntity commandToCommandEntity(Command command);

    Command commandEntityToCommand(CommandEntity commandEntity);

    CertificateEntrySpi certificateEntryToCertificateEntrySpi(CertificateEntry certificateEntry);

    CertificateEntry certificateEntrySpiToCertificateEntry(CertificateEntrySpi certificateEntrySpi);

    ErrorLogEntry errorLogROToErrorLogEntry(ErrorLogRO errorLogRO);

    ErrorLogRO errorLogEntryToErrorLogRO(ErrorLogEntry errorLogEntry);

    PartyResponseRo partyToPartyResponseRo(Party party);

    Party partyResponseRoToParty(PartyResponseRo partyResponseRo);

    eu.domibus.api.usermessage.domain.UserMessage userMessageToUserMessageApi(UserMessage userMessage);

    UserMessage userMessageApiToUserMessage(eu.domibus.api.usermessage.domain.UserMessage userMessage);

    default DomibusPropertyRO propertyApiToPropertyRO(DomibusProperty entity) {
        DomibusPropertyRO res = propertyMetadataApiToPropertyRO(entity.getMetadata());
        res.setValue(entity.getValue());
        return res;
    }

    @Mapping(target = "usageText", expression = "java( meta.getUsageText() )")
    DomibusPropertyRO propertyMetadataApiToPropertyRO(DomibusPropertyMetadata meta);

    default DomibusPropertyTypeRO domibusPropertyMetadataTypeTOdomibusPropertyTypeRO(DomibusPropertyMetadata.Type type){
        return new DomibusPropertyTypeRO(type.name(), type.getRegularExpression());
    }

    @InheritInverseConfiguration
    DomibusPropertyMetadata propertyMetadataDTOTopropertyMetadata(DomibusPropertyMetadataDTO src);

    DomibusPropertiesFilter domibusPropertyFilterRequestTOdomibusPropertiesFilter(PropertyFilterRequestRO source);

    PasswordEncryptionResultDTO passwordEncryptionResultToPasswordEncryptionResultDTO(PasswordEncryptionResult passwordEncryptionResult);

    //cloning mappings
    DomibusPropertyMetadata clonePropertyMetadata(DomibusPropertyMetadata src);

    //list mappings
    List<AuditResponseRo> auditLogListToAuditResponseRoList(List<AuditLog> auditLogList);
    List<AuditLog> auditLogListToAuditList(List<Audit> auditList);

    List<eu.domibus.core.alerts.model.service.Alert> alertPersistListToAlertServiceList(List<Alert> alertList);

    List<AuthenticationEntity> pluginUserROListToAuthenticationEntityList(List<PluginUserRO> pluginUserROList);

    List<BackendFilterEntity> backendFilterListToBackendFilterEntityList(List<BackendFilter> backendFilterList);
    List<BackendFilter> backendFilterEntityListToBackendFilterList(List<BackendFilterEntity> backendFilterEntityList);

    List<BackendFilter> messageFilterROListToBackendFilterList(List<MessageFilterRO> messageFilterROList);
    List<MessageFilterRO> backendFilterListToMessageFilterROList(List<BackendFilter> backendFilterList);

    List<Command> commandEntityListToCommandList(List<CommandEntity> commandEntityList);

    List<DomainRO> domainListToDomainROList(List<Domain> domainList);

    List<DomibusPropertyRO> domibusPropertyListToDomibusPropertyROList(List<DomibusProperty> domibusPropertyList);

    List<DomibusPropertyTypeRO> domibusPropertyMetadataTypeListToDomibusPropertyTypeROList(List<DomibusPropertyMetadata.Type> domibusPropertyMetadataTypeList);

    List<ErrorLogRO> errorLogEntryListToErrorLogROList(List<ErrorLogEntry> errorLogEntryList);

    List<MessageAttempt> messageAttemptEntityListToMessageAttemptList(List<MessageAttemptEntity> messageAttemptEntityList);
    List<MessageAttemptEntity> messageAttemptListToMessageAttemptEntityList(List<MessageAttempt> messageAttemptList);

    List<LoggingLevelRO> loggingEntryListToLoggingLevelROList(List<LoggingEntry> loggingEntryList);

    List<Party> configurationPartyListToPartyList(List<eu.domibus.common.model.configuration.Party> configurationPartyList);
    List<eu.domibus.common.model.configuration.Party> partyListToConfigurationPartyList(List<Party> partyList);

    List<PartyResponseRo> partyListToPartyResponseRoList(List<Party> partyList);
    List<Party> partyResponseRoListToPartyList(List<PartyResponseRo> partyResponseRoList);

    List<eu.domibus.api.process.Process> processListToProcessAPIList(List<Process> processList);

    List<eu.domibus.api.process.Process> processRoListToProcessAPIList(List<ProcessRo> processRoList);
    List<ProcessRo> processAPIListToProcessRoList(List<eu.domibus.api.process.Process> processList);

    List<User> userResponseROListToUserList(List<UserResponseRO> userResponseROList);
    List<UserResponseRO> userListToUserResponseROList(List<User> userList);

    List<PModeResponseRO> pModeArchiveInfoListToPModeResponseROList(List<PModeArchiveInfo> pModeArchiveInfoList);

    List<TrustStoreRO> trustStoreEntryListToTrustStoreROList(List<TrustStoreEntry> trustStoreEntryList);

}

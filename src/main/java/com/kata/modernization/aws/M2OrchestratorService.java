package com.kata.modernization.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.*;

import java.util.List;

@Service
public class M2OrchestratorService {

    private final String accessKey;
    private final String secretKey;
    private final Region region;
    private final M2Client m2Client;

    public M2OrchestratorService(
            @Value("${aws.accessKey}") String accessKey,
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.region}") String regionName) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = Region.of(regionName);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(this.accessKey, this.secretKey);

        this.m2Client = M2Client.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    public List<ApplicationSummary> listM2Applications() {
        ListApplicationsRequest request = ListApplicationsRequest.builder().build();
        ListApplicationsResponse response = m2Client.listApplications(request);
        return response.applications();
    }

    public String startBatchJob(String applicationId, String batchJobIdentifier) {
        StartBatchJobRequest request = StartBatchJobRequest.builder()
                .applicationId(applicationId)
                .batchJobIdentifier(BatchJobIdentifier.builder()
                        .scriptBatchJobIdentifier(ScriptBatchJobIdentifier.builder()
                                .scriptName(batchJobIdentifier)
                                .build())
                        .build())
                .build();

        StartBatchJobResponse response = m2Client.startBatchJob(request);
        return response.executionId();
    }

    public GetApplicationResponse getApplicationDetails(String applicationId) {
        GetApplicationRequest request = GetApplicationRequest.builder()
                .applicationId(applicationId)
                .build();
        return m2Client.getApplication(request);
    }

    public CreateApplicationResponse createM2Application(String name, String description, String definitionContent) {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .name(name)
                .description(description)
                .engineType(EngineType.BLUAGE)
                .definition(Definition.builder()
                        .content(definitionContent)
                        .build())
                .build();

        return m2Client.createApplication(request);
    }
}

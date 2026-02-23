package com.kata.modernization.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import java.util.List;

@Service
public class M2OrchestratorService {

    private final String accessKey;
    private final String secretKey;
    private final Region region;
    private final M2Client m2Client;
    private final boolean simulationMode;

    public M2OrchestratorService(
            @Value("${aws.accessKey}") String accessKey,
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.region}") String regionName,
            @Value("${aws.m2.simulation:false}") boolean simulationMode) {

        this.accessKey = accessKey.trim();
        this.secretKey = secretKey.trim();
        this.region = Region.of(regionName.trim());
        this.simulationMode = simulationMode;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(this.accessKey, this.secretKey);

        this.m2Client = M2Client.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();
    }

    public CreateApplicationResponse createM2Application(String name, String description, String definitionContent) {
        if (simulationMode) {
            return CreateApplicationResponse.builder()
                    .applicationId("sim-app-" + System.currentTimeMillis())
                    .applicationVersion(1)
                    .build();
        }
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .name(name)
                .description(description)
                .engineType(EngineType.MICROFOCUS)
                .roleArn("arn:aws:iam::822754281071:role/M2_Execution_Role")
                .definition(Definition.builder()
                        .content(definitionContent)
                        .build())
                .build();

        return m2Client.createApplication(request);
    }

    public String getCallerIdentity() {
        try (StsClient stsClient = StsClient.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(this.accessKey, this.secretKey)))
                .httpClientBuilder(ApacheHttpClient.builder())
                .build()) {
            GetCallerIdentityResponse response = stsClient.getCallerIdentity();
            return response.arn();
        } catch (Exception e) {
            return "Error al obtener identidad: " + e.getMessage();
        }
    }

    public List<ApplicationSummary> listM2Applications() {
        if (simulationMode) {
            return List.of(
                    ApplicationSummary.builder()
                            .applicationId("sim-app-123")
                            .name("Simulated Application")
                            .status(ApplicationLifecycle.RUNNING)
                            .build());
        }
        ListApplicationsRequest request = ListApplicationsRequest.builder().build();
        ListApplicationsResponse response = m2Client.listApplications(request);
        return response.applications();
    }

    public String startBatchJob(String applicationId, String batchJobIdentifier) {
        if (simulationMode) {
            return "sim-exec-456";
        }
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
        if (simulationMode) {
            return GetApplicationResponse.builder()
                    .applicationId(applicationId)
                    .name("Simulated Application")
                    .status(ApplicationLifecycle.RUNNING)
                    .engineType(EngineType.BLUAGE)
                    .build();
        }
        GetApplicationRequest request = GetApplicationRequest.builder()
                .applicationId(applicationId)
                .build();
        return m2Client.getApplication(request);
    }

}

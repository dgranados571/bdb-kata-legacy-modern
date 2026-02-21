package com.kata.modernization.aws;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.m2.M2Client;
import software.amazon.awssdk.services.m2.model.*;

import java.util.List;

@Service
public class M2OrchestratorService {

    private final M2Client m2Client;

    public M2OrchestratorService() {
        this.m2Client = M2Client.builder().build();
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
}

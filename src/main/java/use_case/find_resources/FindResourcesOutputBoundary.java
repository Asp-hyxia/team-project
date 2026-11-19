package use_case.find_resources;

public interface FindResourcesOutputBoundary {
    void present(FindResourcesOutputData output);
    void presentFailure(DiscoveryException.Code code);
}

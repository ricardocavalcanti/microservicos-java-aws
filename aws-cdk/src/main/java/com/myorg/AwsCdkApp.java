package com.myorg;

import software.amazon.awscdk.App;

public class AwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        VpcStack vpcStack = new VpcStack(app, "vpcStack");

        ClusterStack clusterStack = new ClusterStack(app, "clusterStack", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        Service01Stack service01Stack = new Service01Stack(app, "service01Stack", clusterStack.getCluster());
        service01Stack.addDependency(clusterStack);

        app.synth();
    }
}


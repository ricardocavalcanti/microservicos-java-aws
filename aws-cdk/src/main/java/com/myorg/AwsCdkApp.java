package com.myorg;

import software.amazon.awscdk.App;

public class AwsCdkApp {
    public static void main(final String[] args) {
        App app = new App(); 
        VpcStack vpcStack = new VpcStack(app, "vpcStack");
        ClusterStack clusterStack = new ClusterStack(app, "clusterStack", vpcStack.getVpc());
        // Informa que so ira criar a stackCluster se houver a vpcStack criada antes
        clusterStack.addDependency(vpcStack);
        app.synth();
    }
}


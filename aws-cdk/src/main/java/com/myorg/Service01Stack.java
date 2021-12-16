package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class Service01Stack extends Stack {


	public Service01Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);

        ApplicationLoadBalancedFargateService service01 =
                ApplicationLoadBalancedFargateService.Builder
                        .create(this, "load-balance-01")
                        .serviceName("service-01")
                        .cluster(cluster)
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        // Quantidade de estancia que vai ser executada
                        .desiredCount(2)
                        // Porta liberada para acesso externo
                        .listenerPort(8080)
                        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                                // Aqui definimos o nome do container que queremos
                                .containerName("aws_project01")
                                // Esse valor encontramos no Dockerhub
                                .image(ContainerImage.fromRegistry("rcardcavalcanti/aws_project01:1.0.0"))
                                .containerPort(8080)
                                // Definimos onde vai aparecer os logs da aplicacao e como vao ser criados
                                // Vao ser redirecionados para o servico CloudWatch
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                                .logGroup(LogGroup.Builder.create(this, "Service01LogGroup")
                                                        // Definimos nome que vai agrupar logs
                                                        .logGroupName("Service01")
                                                        // Se precisar vai apagar quando apagar servico
                                                        .removalPolicy(RemovalPolicy.DESTROY)
                                                        .build())
                                                // Dentro do agrupamento de log, sao arquivos reciclados de tempo em tempo
                                                .streamPrefix("Service01")
                                        .build()))
                                .build())
                        .publicLoadBalancer(true)
                        .build();

        // Monitoramento se a aplicacao esta rodando
        // localhost:8080/actuator/health  essa configuracao foi feita no app "aws_project01"
        service01.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                        .path("/actuator/health")
                        .port("8080")
                        .healthyHttpCodes("200")
                .build());

        // Define a capaciadade minima e maxima de auto scale em quantidade de estancias
        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                        .minCapacity(2)
                        .maxCapacity(4)
                .build());

        // Define os parametros de atuação do auto escale referente a processamento da CPU
        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                        // Percentual de uso da CPU
                        .targetUtilizationPercent(50)
                        // Se ficar 60 segundos acima dos 50 % cria as instancias ate o maximo de 4
                        .scaleInCooldown(Duration.seconds(60))
                        // Se ficar 60 segundos abaixo dos 50 % destroe as instancias ate o minino de 2
                        .scaleOutCooldown(Duration.seconds(60))
                .build());
    }
}

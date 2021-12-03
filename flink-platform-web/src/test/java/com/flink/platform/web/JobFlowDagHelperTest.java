package com.flink.platform.web;

import com.flink.platform.common.graph.DAG;
import com.flink.platform.common.model.JobEdge;
import com.flink.platform.common.model.JobVertex;
import com.flink.platform.web.util.JobFlowDagHelper;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static com.flink.platform.common.enums.ExecutionStatus.FAILED;
import static com.flink.platform.common.enums.ExecutionStatus.SUCCEEDED;
import static com.flink.platform.common.model.ExecutionCondition.OR;

/** job flow dag helper test. */
public class JobFlowDagHelperTest {

    private DAG<Long, JobVertex, JobEdge> dag;

    @Before
    public void before() {
        dag = new DAG<>();
        JobVertex jobVertex1 = new JobVertex(1L, 1L);
        JobVertex jobVertex2 = new JobVertex(2L, 2L);
        JobVertex jobVertex3 = new JobVertex(3L, 3L);
        JobVertex jobVertex4 = new JobVertex(4L, 4L);
        JobVertex jobVertex5 = new JobVertex(5L, 5L);
        JobVertex jobVertex6 = new JobVertex(6L, 6L);
        jobVertex6.setPrecondition(OR);
        dag.addVertex(jobVertex1);
        dag.addVertex(jobVertex2);
        dag.addVertex(jobVertex3);
        dag.addVertex(jobVertex4);
        dag.addVertex(jobVertex5);
        dag.addVertex(jobVertex6);
        JobEdge jobEdge1 = new JobEdge(1L, 3L, SUCCEEDED);
        JobEdge jobEdge2 = new JobEdge(2L, 3L, SUCCEEDED);
        JobEdge jobEdge3 = new JobEdge(2L, 4L, SUCCEEDED);
        JobEdge jobEdge4 = new JobEdge(3L, 5L, SUCCEEDED);
        JobEdge jobEdge5 = new JobEdge(4L, 5L, SUCCEEDED);
        JobEdge jobEdge6 = new JobEdge(1L, 6L, FAILED);
        JobEdge jobEdge7 = new JobEdge(2L, 6L, FAILED);
        JobEdge jobEdge8 = new JobEdge(3L, 6L, FAILED);
        JobEdge jobEdge9 = new JobEdge(4L, 6L, FAILED);
        JobEdge jobEdge10 = new JobEdge(5L, 6L, FAILED);
        dag.addEdge(jobEdge1);
        dag.addEdge(jobEdge2);
        dag.addEdge(jobEdge3);
        dag.addEdge(jobEdge4);
        dag.addEdge(jobEdge5);
        dag.addEdge(jobEdge6);
        dag.addEdge(jobEdge7);
        dag.addEdge(jobEdge8);
        dag.addEdge(jobEdge9);
        dag.addEdge(jobEdge10);
    }

    @Test
    public void test1() {
        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test2() {
        dag.getVertex(1L).setSubmitTime(LocalDateTime.now());
        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test3() {
        dag.getVertex(1L).setSubmitTime(LocalDateTime.now());
        dag.getVertex(2L).setSubmitTime(LocalDateTime.now());
        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test4() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test5() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);
        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test6() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(3L);
        vertex.setSubmitTime(LocalDateTime.now());

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test7() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(3L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test8() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(3L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(4L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test9() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(3L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(4L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        vertex = dag.getVertex(5L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    // TODO
    @Test
    public void test10() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(FAILED);

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test11() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(FAILED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(FAILED);

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test12() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(FAILED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }

    @Test
    public void test13() {
        JobVertex vertex = dag.getVertex(2L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(FAILED);

        vertex = dag.getVertex(1L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(FAILED);

        vertex = dag.getVertex(6L);
        vertex.setSubmitTime(LocalDateTime.now());
        vertex.setJobRunStatus(SUCCEEDED);

        Set<JobVertex> executableVertices = JobFlowDagHelper.getExecutableVertices(dag);
        System.out.println(executableVertices);
    }
}
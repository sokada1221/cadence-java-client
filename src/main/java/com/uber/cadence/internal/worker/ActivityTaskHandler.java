/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.worker;

import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.RespondActivityTaskCanceledRequest;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.m3.tally.Scope;
import java.time.Duration;

/**
 * Interface of an activity task handler.
 *
 * @author fateev
 */
public interface ActivityTaskHandler {

  final class Result {

    private final RespondActivityTaskCompletedRequest taskCompleted;
    private final TaskFailedResult taskFailed;
    private final RespondActivityTaskCanceledRequest taskCancelled;
    private int attempt;
    private Duration backoff;

    public static class TaskFailedResult {
      private final RespondActivityTaskFailedRequest taskFailedRequest;
      private final Throwable failure;

      public TaskFailedResult(
          RespondActivityTaskFailedRequest taskFailedRequest, Throwable failure) {
        this.taskFailedRequest = taskFailedRequest;
        this.failure = failure;
      }

      public RespondActivityTaskFailedRequest getTaskFailedRequest() {
        return taskFailedRequest;
      }

      public Throwable getFailure() {
        return failure;
      }
    }

    /**
     * Only zero (manual activity completion) or one request is allowed. Task token and identity
     * fields shouldn't be filled in. Retry options are the service call. These options override the
     * default ones set on the activity worker.
     */
    public Result(
        RespondActivityTaskCompletedRequest taskCompleted,
        TaskFailedResult taskFailed,
        RespondActivityTaskCanceledRequest taskCancelled) {
      this.taskCompleted = taskCompleted;
      this.taskFailed = taskFailed;
      this.taskCancelled = taskCancelled;
    }

    public RespondActivityTaskCompletedRequest getTaskCompleted() {
      return taskCompleted;
    }

    public TaskFailedResult getTaskFailedResult() {
      return taskFailed;
    }

    public RespondActivityTaskCanceledRequest getTaskCancelled() {
      return taskCancelled;
    }

    public void setAttempt(int attempt) {
      this.attempt = attempt;
    }

    public int getAttempt() {
      return attempt;
    }

    public void setBackoff(Duration backoff) {
      this.backoff = backoff;
    }

    public Duration getBackoff() {
      return backoff;
    }
  }

  /**
   * The implementation should be called when a polling activity worker receives a new activity
   * task. This method shouldn't throw any exception unless there is a need to not reply to the
   * task.
   *
   * @param activityTask activity task which is response to PollForActivityTask call.
   * @return One of the possible decision task replies.
   */
  Result handle(
      PollForActivityTaskResponse activityTask, Scope metricsScope, boolean isLocalActivity);

  /** True if this handler handles at least one activity type. */
  boolean isAnyTypeSupported();
}

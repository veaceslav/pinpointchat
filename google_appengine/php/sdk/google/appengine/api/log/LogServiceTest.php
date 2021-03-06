<?php
/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * PHP Unit tests for the LogService.
 *
 */

namespace google\appengine\api\log;

require_once 'google/appengine/api/logservice/log_service_pb.php';
require_once 'google/appengine/api/log/LogService.php';
require_once 'google/appengine/api/log/RequestLog.php';
require_once 'google/appengine/testing/ApiProxyTestBase.php';

use google\appengine\LogOffset;
use google\appengine\LogReadRequest;
use google\appengine\LogReadResponse;
use google\appengine\LogServiceError\ErrorCode;
use google\appengine\runtime\ApplicationError;
use google\appengine\testing\ApiProxyTestBase;

class LogServiceTest extends ApiProxyTestBase {

  const APPLICATION_ID = 'logs-test';
  const VERSION_ID = '2.56789';
  const MAJOR_VERSION = '2'; // derived from VERSION_ID

  const RPC_PACKAGE = 'logservice';
  const RPC_READ_METHOD = 'Read';
  const DEFAULT_BATCH_SIZE = 20;

  public function setUp() {
    parent::setUp();
    error_reporting(E_ALL);
    putenv('APPLICATION_ID='. self::APPLICATION_ID);
    putenv('CURRENT_VERSION_ID='. self::VERSION_ID);
    putenv('CURRENT_MODULE_ID='. 'default');
    // Default timezone must be set for DateTime
    date_default_timezone_set('UTC');
  }

  private function createDefaultRequest($module = null) {
    $request = new LogReadRequest();
    $request->setAppId(self::APPLICATION_ID);
    $mv = $request->addModuleVersion();
    if (isset($module)) {
      $mv->setModuleId($module);
    }
    $mv->setVersionId(self::MAJOR_VERSION);
    $request->setIncludeIncomplete(false);
    $request->setCount(self::DEFAULT_BATCH_SIZE);
    $request->setIncludeAppLogs(false);

    return $request;
  }

  private function populateLogPb($log_pb, $index) {
    $log_pb->setCombined((string) $index);
    $log_pb->mutableOffset()->setRequestId((string) $index);
  }

  private function checkRequestLog($log, $index) {
    $this->assertEquals((string) $index, $log->getCombined());
    $this->assertNotNull($log->getOffset());
  }

  public function testFetchAllLogs($module = null) {
    $num_results = self::DEFAULT_BATCH_SIZE * 1.5;

    self::expectTwoBatchFetch($num_results, $module);

    $index = 0;
    $iterator = LogService::fetch();
    foreach ($iterator as $log) {
      self::checkRequestLog($log, $index);
      $index++;
    }
    $this->assertEquals($num_results, $index);
    $this->apiProxyMock->verify();

    // Iterate again with the same iterator and expect more API calls
    self::expectTwoBatchFetch($num_results, $module);
    $index = 0;
    foreach ($iterator as $log) {
      self::checkRequestLog($log, $index);
      $index++;
    }
    $this->assertEquals($num_results, $index);
    $this->apiProxyMock->verify();
  }

  // Create two requests and two responses
  private function expectTwoBatchFetch($num_results, $module) {
    $cursor = (string) self::DEFAULT_BATCH_SIZE;
    $first_request = $this->createDefaultRequest($module);
    $first_response = new LogReadResponse();
    for ($i = 0; $i < self::DEFAULT_BATCH_SIZE; $i++) {
      self::populateLogPb($first_response->addLog(), $i);
    }
    $first_response->mutableOffset()->setRequestId($cursor);

    $second_request = $this->createDefaultRequest($module);
    $second_request->mutableOffset()->setRequestId($cursor);

    $secondResponse = new LogReadResponse();
    for ($i = self::DEFAULT_BATCH_SIZE;
        $i < $num_results;
        $i++) {
      self::populateLogPb($secondResponse->addLog(), $i);
    }

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $first_request,
        $first_response);

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $second_request,
        $secondResponse);
  }

  public function testAllLogsNonDefaultModule() {
    putenv('CURRENT_MODULE_ID='. 'someOtherModule');
    self::testFetchAllLogs('someOtherModule');
    putenv('CURRENT_MODULE_ID='. 'default');
  }

  public function testSetStartAndEndTime() {
    $end = new \DateTime('2013-06-25');
    $start = clone $end;

    // Start time is 10 secs before end time.
    $start->sub(\DateInterval::createFromDateString('10 seconds'));

    $request = $this->createDefaultRequest();
    $request->setStartTime($start->getTimeStamp() * 1e6);
    $request->setEndTime($end->getTimeStamp() * 1e6);

    $response = new LogReadResponse();
    for ($i = 0; $i < 3; $i++) {
      $log_pb = $response->addLog();
      $log_pb->setStartTime($this->usecs($start, $i) -1e6); // sub 1 sec
      $log_pb->setEndTime($this->usecs($start, $i));
    }

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    // Test both DateTime and integer time types.
    $options = [
      'start_time' => $start,
      'end_time' => $end->getTimestamp() * 1e6
    ];

    $count = 0;
    foreach (LogService::fetch($options) as $log) {
      $this->assertGreaterThanOrEqual($start, $log->getEndDateTime());
      $this->assertLessThanOrEqual($end, $log->getEndDateTime());
      $this->assertGreaterThanOrEqual(
          $log->getStartDateTime()->getTimestamp() * 1e6,
          $log->getStartTimeUsec());
      $count++;
    }

    $this->assertEquals(3, $count);

    $this->apiProxyMock->verify();
  }

  /**
   * Create a microsecond time by adding 5 seconds per index to base DateTime.
   */
  private function usecs($time, $index) {
    return ($time->getTimestamp() + $index * 5) * 1e6;
  }

  public function testAppLogs() {
    $request = $this->createDefaultRequest();
    $request->setIncludeAppLogs(true);

    $start = new \DateTime('2013-06-25');

    $response = new LogReadResponse();
    for ($i = 0; $i < 3; $i++) {
      $log_pb = $response->addLog();
      for ($j = 0; $j < 5; $j++) {
        $line_pb = $log_pb->addLine();
        $line_pb->setLevel($j);
        $line_pb->setLogMessage("Log $j");
        $line_pb->setTime($this->usecs($start, $i, $j / 5));
      }
    }

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $options = ['include_app_logs' => true];

    $i = 0;
    foreach (LogService::fetch($options) as $log) {
      $lines = $log->getAppLogs();
      $j = 0;
      foreach ($lines as $line) {
        $this->assertEquals($j, $line->getLevel());
        $this->assertEquals("Log $j", $line->getMessage());
        $this->assertEquals($this->usecs($start, $i, $j / 5),
            $line->getTimeUsec());
        $this->assertGreaterThanOrEqual($start, $line->getDateTime());
        $j++;
      }
      $i++;
    }

    $this->apiProxyMock->verify();
  }

  public function testIncludeIncomplete() {
    $request = $this->createDefaultRequest();
    $request->setIncludeIncomplete(true);
    $response = new LogReadResponse();
    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $options = ['include_incomplete' => true];

    $result = LogService::fetch($options);
    $this->assertInstanceOf('Iterator', $result);
    $result->rewind();
    $this->assertFalse($result->valid());

    $this->apiProxyMock->verify();
  }

  public function testMinimumLogLevel() {
    self::doTestMinimumLogLevel(LogService::LEVEL_CRITICAL);
    self::doTestMinimumLogLevel(LogService::LEVEL_DEBUG);
  }

  private function doTestMinimumLogLevel($level) {
    $request = $this->createDefaultRequest();
    $request->setMinimumLogLevel($level);
    $response = new LogReadResponse();
    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $options = ['minimum_log_level' => $level];

    $result = LogService::fetch($options);
    $this->assertInstanceOf('Iterator', $result);
    $result->rewind();
    $this->assertFalse($result->valid());

    $this->apiProxyMock->verify();
  }

  public function testVersionIds() {
    $request = $this->createDefaultRequest();
    $request->clearModuleVersion();

    $request->addModuleVersion()->setVersionId('v1');
    $request->addModuleVersion()->setVersionId('v2');
    $request->addModuleVersion()->setVersionId('v3');

    $response = new LogReadResponse();

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $options = ['versions' => ['v1', 'v2', 'v3']];

    $result = LogService::fetch($options);

    $result->rewind();
    $this->assertFalse($result->valid());

    $this->apiProxyMock->verify();
  }

  public function testModuleVersions() {
    $request = $this->createDefaultRequest();
    $request->clearModuleVersion();

    $mv = $request->addModuleVersion();
    $mv->setModuleId("m1");
    $mv->setVersionId("v1");
    $mv = $request->addModuleVersion();
    $mv->setModuleId("m1");
    $mv->setVersionId("v2");
    $mv = $request->addModuleVersion();
    $mv->setModuleId("m2");
    $mv->setVersionId("v3");

    $response = new LogReadResponse();

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    // Test both multiple versions and a single version.
    $options = [
      'module_versions' =>
      ["m1" => ["v1", "v2"], "m2" => "v3"]
    ];

    $result = LogService::fetch($options);

    $result->rewind();
    $this->assertFalse($result->valid());

    $this->apiProxyMock->verify();
  }

  public function testBatchSize() {
    $request = $this->createDefaultRequest();
    $request->setCount(50);

    $response = new LogReadResponse();

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $options = ['batch_size' => 50];
    $result = LogService::fetch($options);
    $result->rewind();
    $this->assertFalse($result->valid());

    $this->apiProxyMock->verify();
  }

  public function testOffsetEncoding() {
    $unsafe = ' +/=';

    $request = $this->createDefaultRequest();
    $response = new LogReadResponse();
    $response->addLog()->mutableOffset()->setRequestId($unsafe);

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    // Will only be a single result.
    $offset;
    foreach (LogService::fetch() as $log) {
      $offset = $log->getOffset();

      // Ensure certain non-url safe characters are not present
      $this->assertFalse(strpbrk($offset, ' +/=:'));
    }

    $this->apiProxyMock->verify();

    // Make another call using the current offset
    $request->mutableOffset()->setRequestId($unsafe);
    $response = new LogReadResponse();

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $options = ['offset' => $offset];

    // No results but need loop to trigger Api call
    foreach (LogService::fetch($options) as $log) {
    }

    $this->apiProxyMock->verify();
  }

  public function testInvalidStart() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['start_time' => 'wrong'];
    LogService::fetch($options);
  }

  public function testInvalidLevel() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['minimum_log_level' => 10];
    LogService::fetch($options);
  }

  public function testInvalidIncomplete() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['include_imcomplete' => 10];
    LogService::fetch($options);
  }

  public function testInvalidOffset() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['offset' => 10];
    LogService::fetch($options);
  }

  public function testInvalidVersionIdTypes() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['versions' => [5, 20, 40]];
    LogService::fetch($options);
  }

  public function testInvalidVersionIds() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['versions' => ['thisIsOk', 'this one is not']];
    LogService::fetch($options);
  }

  public function testInvalidModules() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['module_versions' => ["foo" => false, "bar" => 9]];
    LogService::fetch($options);
  }

  public function testOversizeBatch() {
    $this->setExpectedException('InvalidArgumentException');
    $options = ['batch_size' => LogService::MAX_BATCH_SIZE + 1];
    LogService::fetch($options);
  }

  public function testSetModuleVersionsAndVersions() {
    $this->setExpectedException('InvalidArgumentException');
    $options = [
      'versions' => ["foo", "bar"],
      'module_veresions' => ["1" => "a", "2" => "b"],
    ];
    LogService::fetch($options);
  }

  public function testFetchByMultipleIds() {
    $ids = [];
    $request = new LogReadRequest();
    $request->setAppId(self::APPLICATION_ID);
    $request->setIncludeAppLogs(true);
    $request->addModuleVersion()->setVersionId(self::MAJOR_VERSION);
    $response = new LogReadResponse();
    for ($i = 0; $i < 5; $i++) {
      $ids[] = sprintf('%d', $i);
      $request->addRequestId(sprintf('%d', $i));
      self::populateLogPb($response->addLog(), $i);
    }

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $index = 0;
    foreach (LogService::fetchById($ids) as $log) {
      self::checkRequestLog($log, $index);
      $index++;
    }
    $this->assertEquals(5, $index);
    $this->apiProxyMock->verify();
  }

  public function testFetchBySingleId() {
    $request = new LogReadRequest();
    $request->setAppId(self::APPLICATION_ID);
    $request->setIncludeAppLogs(true);
    $request->addModuleVersion()->setVersionId(self::MAJOR_VERSION);
    $response = new LogReadResponse();
    $request->addRequestId("1A");
    self::populateLogPb($response->addLog(), 10);

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $index = 0;
    foreach (LogService::fetchById("1A") as $log) {
      self::checkRequestLog($log, 10);
      $index++;
    }
    $this->assertEquals(1, $index);
    $this->apiProxyMock->verify();
  }

  public function testFetchByIdsWithAppLogs() {
    $ids = [];
    $request = new LogReadRequest();
    $request->setAppId(self::APPLICATION_ID);
    $request->setIncludeAppLogs(true);
    $request->addModuleVersion()->setVersionId(self::MAJOR_VERSION);
    $response = new LogReadResponse();
    for ($i = 0; $i < 5; $i++) {
      $ids[] = sprintf('%d', $i);
      $request->addRequestId(sprintf('%d', $i));
      $this->populateLogPb($response->addLog(), $i);
    }

    $this->apiProxyMock->expectCall(
        self::RPC_PACKAGE,
        self::RPC_READ_METHOD,
        $request,
        $response);

    $index = 0;
    foreach (LogService::fetchById($ids, true) as $log) {
      $this->checkRequestLog($log, $index);
      $index++;
    }
    $this->assertEquals(5, $index);
    $this->apiProxyMock->verify();
  }

  public function testInvalidRequestId() {
    $this->setExpectedException('InvalidArgumentException');
    // Request Ids must be hex values
    LogService::fetchById("T2");
  }

  public function testApplicationError() {
    $request = $this->createDefaultRequest();
    $exception = new ApplicationError(ErrorCode::INVALID_REQUEST, "test");
    $this->setExpectedException('\google\appengine\api\log\LogException',
                                'Invalid Request');

    $this->apiProxyMock->expectCall(self::RPC_PACKAGE,
                                    self::RPC_READ_METHOD,
                                    $request,
                                    $exception);

    foreach (LogService::fetch() as $logs) {
      // Should never reach this due to exception.
    }
  }

  public function testIteratorNextCalledAfter() {
    $request = $this->createDefaultRequest();
    $response = new LogReadResponse();
    $response->addLog();
    $response->addLog();
    $response->addLog();

    $this->setExpectedException('\LogicException', 'Invalid iterator state');

    $this->apiProxyMock->expectCall(self::RPC_PACKAGE,
                                    self::RPC_READ_METHOD,
                                    $request,
                                    $response);
    $iterator = LogService::fetch();
    foreach ($iterator as $log) {
    }

    $iterator->next();
  }

  public function testGetAppEngineLogLevel() {
    $this->assertEquals(0, LogService::getAppEngineLogLevel(LOG_DEBUG));
    $this->assertEquals(LogService::LEVEL_CRITICAL,
                        LogService::getAppEngineLogLevel(LOG_EMERG));
  }
}

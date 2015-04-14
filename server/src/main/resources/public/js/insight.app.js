'use strict';

var asioApp = angular.module('asioApp', []);

asioApp.controller('EventMonitor', function ($scope) {
  $scope.events = [];
  var handler = function (event) {
    $scope.$apply(function () {
      $scope.events.push(event);
    });
  }
  var feed = new EventSource('/api/events');
  feed.addEventListener('request', handler, false);
  feed.addEventListener('stream', handler, false);
});

//pragma solidity ^0.4.0;

contract Dest {
    event logFallback(uint256 indexed, uint256 indexed, uint256 indexed);
 event logGetToken(uint256 indexed, uint256 indexed, uint256 indexed, uint256);

 function Dest() payable public {}

    function getToken(grcToken tokenId) payable {
        logGetToken(msg.sender.tokenBalance(tokenId), msg.tokenid, msg.tokenvalue, msg.value);
 }

    function getTokenLongMin() payable {
        // long.min - 1000020
 logGetToken(msg.sender.tokenBalance(grcToken(-9223372036855775828)), msg.tokenid, msg.tokenvalue, msg.value);
 }

    function getTokenLongMax() payable {
        // long.max + 1000020
 logGetToken(msg.sender.tokenBalance(grcToken(9223372036855775827)), msg.tokenid, msg.tokenvalue, msg.value);
 }

    function () payable {
        logFallback(msg.tokenid, msg.tokenvalue, msg.value);
 }
}
//pragma solidity ^0.4.24;
contract transferTokenContract {
    constructor() payable public{}
    function() payable public{}
    function transferTokenTest(address toAddress, uint256 tokenValue, grcToken id) payable public  {
            toAddress.transferToken(tokenValue, id);
    }
    function transferTokenTestIDOverBigInteger(address toAddress) payable public  {
        toAddress.transferToken(1, 9223372036854775809);
    }
    function transferTokenTestValueRandomIdBigInteger(address toAddress) payable public  {
        toAddress.transferToken(1, 36893488147420103233);
    }
    function msgTokenValueAndTokenIdTest() public payable returns(grcToken, uint256){
        grcToken id = msg.tokenid;
        uint256 value = msg.tokenvalue;
        return (id, value);
    }
    function getTokenBalanceTest(address accountAddress) payable public returns (uint256){
        grcToken id = 1000001;
        return accountAddress.tokenBalance(id);
    }
    function getTokenBalnce(address toAddress, grcToken tokenId) public payable returns(uint256){
        return toAddress.tokenBalance(tokenId);
    }
}
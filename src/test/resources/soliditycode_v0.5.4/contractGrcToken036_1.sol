//pragma solidity ^0.4.24;
contract IllegalDecorate {
constructor() payable public{}
function() payable external{}
event log(uint256);
function transferTokenWithPure(address payable toAddress, uint256 tokenValue) public pure {
emit log(msg.value);
emit log(msg.tokenvalue);
emit log(msg.tokenid);
toAddress.transferToken(msg.tokenvalue, msg.tokenid);
toAddress.transfer(msg.value);
}
}

pragma solidity ^0.4.24;
contract ConvertType {

constructor() payable public{}

function() payable external{}

//function stringToGrctoken(address payable toAddress, string memory tokenStr, uint256 tokenValue) public {
// grcToken t = grcToken(tokenStr); // ERROR
// toAddress.transferToken(tokenValue, tokenStr); // ERROR
//}

function uint256ToGrctoken(address toAddress,uint256 tokenValue, uint256 tokenInt)  public {
  grcToken t = grcToken(tokenInt); // OK
  toAddress.transferToken(tokenValue, t); // OK
  toAddress.transferToken(tokenValue, tokenInt); // OK
}

function addressToGrctoken(address toAddress, uint256 tokenValue, address adr) public {
  grcToken t = grcToken(adr); // OK
  toAddress.transferToken(tokenValue, t); // OK
//toAddress.transferToken(tokenValue, adr); // ERROR
}

//function bytesToGrctoken(address payable toAddress, bytes memory b, uint256 tokenValue) public {
 // grcToken t = grcToken(b); // ERROR
 // toAddress.transferToken(tokenValue, b); // ERROR
//}

function bytes32ToGrctoken(address toAddress, uint256 tokenValue, bytes32 b32) public {
  grcToken t = grcToken(b32); // OK
  toAddress.transferToken(tokenValue, t); // OK
// toAddress.transferToken(tokenValue, b32); // ERROR
}

//function arrayToGrctoken(address payable toAddress, uint256[] memory arr, uint256 tokenValue) public {
//grcToken t = grcToken(arr); // ERROR
// toAddress.transferToken(tokenValue, arr); // ERROR
//}
}
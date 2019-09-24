//pragma solidity ^0.4.24;

contract IllegalDecorate {

    constructor() payable public{}

    function() payable external {}

    function transferTokenWithOutPayable(address payable toAddress, grcToken id, uint256 tokenValue) public {

        toAddress.transferToken(tokenValue, id);
    }
}
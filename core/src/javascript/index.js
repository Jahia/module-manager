import React from 'react';
import ReactDOM from 'react-dom';
import RegistrationComponent from './registrations';

var mountElement = document.createElement('div');
ReactDOM.render(<RegistrationComponent/>, mountElement);

console.log('%c Module Manager routes have been registered', 'color: #3c8cba');
